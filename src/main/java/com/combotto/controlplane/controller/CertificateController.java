package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.validation.Valid;

import com.combotto.controlplane.api.CertificateRenewalHistoryResponse;
import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CertificateSummaryResponse;
import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.common.ApiError;
import com.combotto.controlplane.common.PageableSanitizer;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.services.CertificateRenewalStatusHistoryService;
import com.combotto.controlplane.services.CertificateService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/certificates")
@Tag(name = "Certificates", description = "Manage tenant-scoped certificate inventory, renewal workflow, and search views")
public class CertificateController {

  private final CertificateService certificateService;
  private final CertificateRenewalStatusHistoryService certificateRenewalStatusHistoryService;

  public CertificateController(
      CertificateService certificateService,
      CertificateRenewalStatusHistoryService certificateRenewalStatusHistoryService) {
    this.certificateService = certificateService;
    this.certificateRenewalStatusHistoryService = certificateRenewalStatusHistoryService;
  }

  @Operation(summary = "Create certificate", description = "Registers a new certificate for the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Certificate created"),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping
  public ResponseEntity<CertificateResponse> create(
      @Valid @RequestBody CreateCertificateRequest request,
      UriComponentsBuilder uriBuilder) {

    CertificateResponse created = certificateService.create(request);

    URI location = uriBuilder
        .path("/api/certificates/{id}")
        .buildAndExpand(created.id())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @Operation(summary = "List certificates", description = "Returns paginated certificates for the authenticated tenant with optional filters")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Certificates returned successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid filter values", content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping
  public Page<CertificateResponse> list(
      @Parameter(description = "Optional tenant filter. Must match the authenticated tenant when provided.", example = "demo-tenant") @RequestParam(required = false) String tenantId,
      @Parameter(description = "Optional certificate lifecycle status filter.", example = "ACTIVE") @RequestParam(required = false) CertificateStatus status,
      @Parameter(description = "Optional renewal workflow status filter.", example = "IN_PROGRESS") @RequestParam(required = false) RenewalStatus renewalStatus,
      @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return certificateService.list(
        tenantId,
        status,
        renewalStatus,
        PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.desc("createdAt"))));
  }

  @Operation(summary = "List expiring certificates", description = "Returns certificates expiring within the requested window for the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Expiring certificates returned successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/expiring-soon")
  public Page<CertificateResponse> listExpiringSoon(
      @Parameter(description = "Number of days ahead to inspect for expiry.", example = "30") @RequestParam(defaultValue = "30") int days,
      @Parameter(description = "Optional tenant filter. Must match the authenticated tenant when provided.", example = "demo-tenant") @RequestParam(required = false) String tenantId,
      @Parameter(description = "Optional owner filter.", example = "platform-team") @RequestParam(required = false) String owner,
      @Parameter(description = "Optional renewal workflow status filter.", example = "PLANNED") @RequestParam(required = false) RenewalStatus renewalStatus,
      @ParameterObject @PageableDefault(size = 20, sort = "notAfter", direction = Sort.Direction.ASC) Pageable pageable) {
    return certificateService.listExpiringSoon(
        days,
        tenantId,
        owner,
        renewalStatus,
        PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.asc("notAfter"))));
  }

  @Operation(summary = "List attention-needed certificates", description = "Returns certificates that are expired, blocked, or approaching expiry without enough renewal progress")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Attention-needed certificates returned successfully"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/attention-needed")
  public Page<CertificateResponse> listAttentionNeeded(
      @Parameter(description = "Number of days ahead to consider in the attention window.", example = "30") @RequestParam(defaultValue = "30") int days,
      @ParameterObject @PageableDefault(size = 20, sort = "notAfter", direction = Sort.Direction.ASC) Pageable pageable) {
    return certificateService.listAttentionNeeded(
        days,
        PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.asc("notAfter"))));
  }

  @Operation(summary = "Get certificate", description = "Returns one certificate visible to the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Certificate returned successfully"),
      @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{id}")
  public CertificateResponse getById(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID id) {
    return certificateService.getById(id);
  }

  @Operation(summary = "Update certificate", description = "Partially updates certificate metadata and renewal workflow state")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Certificate updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid update payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PatchMapping("/{id}")
  public CertificateResponse update(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID id,
      @Valid @RequestBody UpdateCertificateRequest request) {
    return certificateService.update(id, request);
  }

  @Operation(summary = "Delete certificate", description = "Deletes a certificate visible to the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Certificate deleted"),
      @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID id) {
    certificateService.delete(id);
  }

  @Operation(summary = "Get certificate summary", description = "Returns aggregate certificate metrics for the authenticated tenant")
  @ApiResponse(responseCode = "200", description = "Summary returned successfully")
  @GetMapping("/summary")
  public CertificateSummaryResponse summary() {
    return certificateService.summary();
  }

  @Operation(
      summary = "Get certificate renewal history",
      description = "Returns renewal workflow history rows for a certificate visible to the authenticated tenant in descending occurredAt order")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Renewal history returned successfully",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = CertificateRenewalHistoryResponse.class)))),
      @ApiResponse(responseCode = "403", description = "Certificate belongs to another tenant", content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{id}/renewal-history")
  public List<CertificateRenewalHistoryResponse> getRenewalHistory(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID id) {
    return certificateRenewalStatusHistoryService.listByCertificateId(id);
  }
}
