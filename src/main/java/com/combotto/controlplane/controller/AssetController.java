package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
import com.combotto.controlplane.services.AssetService;
import com.combotto.controlplane.services.CertificateBindingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetService assetService;
  private final CertificateBindingService certificateBindingService;

  public AssetController(AssetService assetService, CertificateBindingService certificateBindingService) {
    this.assetService = assetService;
    this.certificateBindingService = certificateBindingService;
  }

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

  @GetMapping
  public List<AssetResponse> list() {
    return assetService.list();
  }

  @GetMapping("/{id}")
  public AssetResponse getById(@PathVariable UUID id) {
    return assetService.getById(id);
  }

  @PatchMapping("/{id}")
  public AssetResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateAssetRequest request) {
    return assetService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    assetService.delete(id);
  }

  @GetMapping("/{assetId}/bindings")
  public List<CertificateBindingResponse> listBindingsByAssetId(@PathVariable UUID assetId) {
    return certificateBindingService.listByAssetId(assetId);
  }

  @GetMapping("/{assetId}/certificates")
  public List<CertificateResponse> listCertificatesByAssetId(@PathVariable UUID assetId) {
    return certificateBindingService.listCertificatesByAssetId(assetId);
  }
}
