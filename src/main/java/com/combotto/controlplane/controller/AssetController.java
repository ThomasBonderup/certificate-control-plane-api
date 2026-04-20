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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.combotto.controlplane.api.AssetResponse;
import com.combotto.controlplane.api.CertificateBindingResponse;
import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.api.UpdateAssetRequest;
import com.combotto.controlplane.common.ApiError;
import com.combotto.controlplane.common.PageableSanitizer;
import com.combotto.controlplane.services.AssetService;
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
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Manage tenant-scoped assets and their certificate relationships")
public class AssetController {

  private final AssetService assetService;
  private final CertificateBindingService certificateBindingService;

  public AssetController(AssetService assetService, CertificateBindingService certificateBindingService) {
    this.assetService = assetService;
    this.certificateBindingService = certificateBindingService;
  }

  @Operation(summary = "Create asset", description = "Registers a new asset for the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Asset created"),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping
  public ResponseEntity<AssetResponse> create(
      @Valid @RequestBody CreateAssetRequest request,
      UriComponentsBuilder uriBuilder) {
    AssetResponse created = assetService.create(request);

    URI location = uriBuilder
        .path("/api/assets/{id}")
        .buildAndExpand(created.id())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @Operation(summary = "List assets", description = "Returns paginated assets for the authenticated tenant")
  @ApiResponse(responseCode = "200", description = "Assets returned successfully")
  @GetMapping
  public Page<AssetResponse> list(
      @ParameterObject
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return assetService.list(PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.desc("createdAt"))));
  }

  @Operation(summary = "Get asset", description = "Returns one asset visible to the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Asset returned successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{id}")
  public AssetResponse getById(@Parameter(description = "Asset identifier.", example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
    return assetService.getById(id);
  }

  @Operation(summary = "Update asset", description = "Partially updates an asset visible to the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Asset updated successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PatchMapping("/{id}")
  public AssetResponse update(
      @Parameter(description = "Asset identifier.", example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID id,
      @Valid @RequestBody UpdateAssetRequest request) {
    return assetService.update(id, request);
  }

  @Operation(summary = "Delete asset", description = "Deletes an asset visible to the authenticated tenant")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Asset deleted"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@Parameter(description = "Asset identifier.", example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
    assetService.delete(id);
  }

  @Operation(summary = "List asset bindings", description = "Returns paginated certificate bindings for a single asset")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bindings returned successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{assetId}/bindings")
  public Page<CertificateBindingResponse> listBindingsByAssetId(
      @Parameter(description = "Asset identifier.", example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID assetId,
      @ParameterObject
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return certificateBindingService.listByAssetId(
        assetId,
        PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.desc("createdAt"))));
  }

  @Operation(summary = "List asset certificates", description = "Returns all certificates currently bound to a single asset")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Certificates returned successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{assetId}/certificates")
  public List<CertificateResponse> listCertificatesByAssetId(
      @Parameter(description = "Asset identifier.", example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID assetId) {
    return certificateBindingService.listCertificatesByAssetId(assetId);
  }
}
