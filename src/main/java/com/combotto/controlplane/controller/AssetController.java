package com.combotto.controlplane.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.combotto.controlplane.api.AssetResponse;
import com.combotto.controlplane.api.CertificateBindingResponse;
import com.combotto.controlplane.api.CertificateResponse;
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
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Read Combotto Monitor assets and their certificate relationships")
public class AssetController {

  private final AssetService assetService;
  private final CertificateBindingService certificateBindingService;

  public AssetController(AssetService assetService, CertificateBindingService certificateBindingService) {
    this.assetService = assetService;
    this.certificateBindingService = certificateBindingService;
  }

  @Operation(summary = "List assets", description = "Returns active Combotto Monitor assets")
  @ApiResponse(responseCode = "200", description = "Assets returned successfully")
  @GetMapping
  public Page<AssetResponse> list(
      @ParameterObject
      @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
    return assetService.list(PageableSanitizer.sanitize(pageable, Sort.by(Sort.Order.asc("name"))));
  }

  @Operation(summary = "Get asset", description = "Returns one active Combotto Monitor asset")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Asset returned successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{id}")
  public AssetResponse getById(@Parameter(description = "Combotto Monitor asset identifier.", example = "7") @PathVariable Long id) {
    return assetService.getById(id);
  }

  @Operation(summary = "List asset bindings", description = "Returns paginated certificate bindings for a single asset")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Bindings returned successfully"),
      @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @GetMapping("/{assetId}/bindings")
  public Page<CertificateBindingResponse> listBindingsByAssetId(
      @Parameter(description = "Combotto Monitor asset identifier.", example = "7") @PathVariable Long assetId,
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
      @Parameter(description = "Combotto Monitor asset identifier.", example = "7") @PathVariable Long assetId) {
    return certificateBindingService.listCertificatesByAssetId(assetId);
  }
}
