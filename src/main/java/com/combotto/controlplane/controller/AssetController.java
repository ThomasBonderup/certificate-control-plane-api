package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.combotto.controlplane.api.AssetResponse;
import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.services.AssetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetService assetService;

  public AssetController(AssetService assetService) {
    this.assetService = assetService;
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
}
