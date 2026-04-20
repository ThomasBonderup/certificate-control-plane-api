package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.combotto.controlplane.api.CertificateBindingResponse;
import com.combotto.controlplane.api.CreateCertificateBindingRequest;
import com.combotto.controlplane.common.ApiError;
import com.combotto.controlplane.common.PageableSanitizer;
import com.combotto.controlplane.services.CertificateBindingService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/certificates/{certificateId}/bindings")
@Tag(name = "Certificate Bindings", description = "Bind certificates to assets, endpoints, and usage contexts")
public class CertificateBindingController {
  private final CertificateBindingService certificateBindingService;

  public CertificateBindingController(CertificateBindingService certificateBindingService) {
    this.certificateBindingService = certificateBindingService;
  }

  @Operation(summary = "Create certificate binding", description = "Creates a new binding between a certificate and an asset")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Binding created"),
      @ApiResponse(responseCode = "404", description = "Certificate or asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping
  public ResponseEntity<CertificateBindingResponse> create(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID certificateId,
      @Valid @RequestBody CreateCertificateBindingRequest request,
      UriComponentsBuilder uriBuilder) {
    CertificateBindingResponse created = certificateBindingService.create(certificateId, request);

    URI location = uriBuilder
        .path("/api/certificates/{certificateId}/bindings")
        .buildAndExpand(certificateId, created.id())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @Operation(summary = "List certificate bindings", description = "Returns paginated bindings for a single certificate")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bindings returned successfully"),
      @ApiResponse(responseCode = "404", description = "Certificate not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping
  public Page<CertificateBindingResponse> listByCertificateId(
      @Parameter(description = "Certificate identifier.", example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID certificateId,
      @ParameterObject
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return certificateBindingService.listByCertificateId(
        certificateId,
        PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.desc("createdAt"))));
  }
}
