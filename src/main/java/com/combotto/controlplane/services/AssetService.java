package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.combotto.controlplane.api.AssetResponse;
import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.api.UpdateAssetRequest;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.model.AssetEntity;
import com.combotto.controlplane.repositories.AssetRepository;

@Service
public class AssetService {

  private final AssetRepository assetRepository;

  public AssetService(AssetRepository assetRepository) {
    this.assetRepository = assetRepository;
  }

  public AssetResponse create(CreateAssetRequest request) {
    OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    AssetEntity entity = new AssetEntity();
    entity.setId(UUID.randomUUID());
    entity.setTenantId(request.tenantId());
    entity.setName(request.name());
    entity.setAssetType(request.assetType());
    entity.setEnvironment(request.environment());
    entity.setHostname(request.hostname());
    entity.setLocation(request.location());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    return toResponse(assetRepository.save(entity));
  }

  public List<AssetResponse> list() {
    return assetRepository.findAll()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public AssetResponse getById(UUID id) {
    AssetEntity entity = assetRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    return toResponse(entity);
  }

  public AssetResponse update(UUID id, UpdateAssetRequest request) {
    AssetEntity entity = assetRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));

    if (request.name() != null)
      entity.setName(request.name());
    if (request.assetType() != null)
      entity.setAssetType(request.assetType());
    if (request.environment() != null)
      entity.setEnvironment(request.environment());
    if (request.hostname() != null)
      entity.setHostname(request.hostname());
    if (request.location() != null)
      entity.setLocation(request.location());

    entity.setUpdatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));

    return toResponse(assetRepository.save(entity));
  }

  public void delete(UUID id) {
    if (!assetRepository.existsById(id)) {
      throw new ResourceNotFoundException("Asset not found: " + id);
    }
    assetRepository.deleteById(id);
  }

  private AssetResponse toResponse(AssetEntity entity) {
    return new AssetResponse(
        entity.getId(),
        entity.getTenantId(),
        entity.getName(),
        entity.getAssetType().name(),
        entity.getEnvironment(),
        entity.getHostname(),
        entity.getLocation(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
