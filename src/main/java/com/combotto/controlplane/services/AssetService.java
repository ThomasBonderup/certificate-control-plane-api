package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.combotto.controlplane.api.AssetResponse;
import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.api.UpdateAssetRequest;
import com.combotto.controlplane.common.CurrentTenantProvider;
import com.combotto.controlplane.common.CurrentUserProvider;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.common.TenantAccessValidator;
import com.combotto.controlplane.model.AssetEntity;
import com.combotto.controlplane.repositories.AssetRepository;

@Service
public class AssetService {

  private final AssetRepository assetRepository;
  private final CurrentUserProvider currentUserProvider;
  private final CurrentTenantProvider currentTenantProvider;
  private final TenantAccessValidator tenantAccessValidator;

  public AssetService(
      AssetRepository assetRepository,
      CurrentUserProvider currentUserProvider,
      CurrentTenantProvider currentTenantProvider,
      TenantAccessValidator tenantAccessValidator) {
    this.assetRepository = assetRepository;
    this.currentUserProvider = currentUserProvider;
    this.currentTenantProvider = currentTenantProvider;
    this.tenantAccessValidator = tenantAccessValidator;
  }

  public AssetResponse create(CreateAssetRequest request) {
    String tenantId = currentTenantProvider.getRequiredTenantId();
    tenantAccessValidator.validateTenantMatch(request.tenantId(), tenantId);
    OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    AssetEntity entity = new AssetEntity();
    entity.setId(UUID.randomUUID());
    entity.setTenantId(tenantId);
    entity.setName(request.name());
    entity.setAssetType(request.assetType());
    entity.setEnvironment(request.environment());
    entity.setHostname(request.hostname());
    entity.setLocation(request.location());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    String currentUser = currentUserProvider.getCurrentUserId();
    entity.setCreatedBy(currentUser);
    entity.setUpdatedBy(currentUser);

    return toResponse(assetRepository.save(entity));
  }

  public Page<AssetResponse> list(Pageable pageable) {
    return assetRepository.findAllByTenantId(currentTenantProvider.getRequiredTenantId(), pageable)
        .map(this::toResponse);
  }

  public AssetResponse getById(UUID id) {
    AssetEntity entity = assetRepository.findByIdAndTenantId(id, currentTenantProvider.getRequiredTenantId())
        .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    return toResponse(entity);
  }

  public AssetResponse update(UUID id, UpdateAssetRequest request) {
    AssetEntity entity = assetRepository.findByIdAndTenantId(id, currentTenantProvider.getRequiredTenantId())
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

    String currentUser = currentUserProvider.getCurrentUserId();
    entity.setUpdatedBy(currentUser);

    return toResponse(assetRepository.save(entity));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID id) {
    if (!assetRepository.existsByIdAndTenantId(id, currentTenantProvider.getRequiredTenantId())) {
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
        entity.getUpdatedAt(),
        entity.getCreatedBy(),
        entity.getUpdatedBy());
  }
}
