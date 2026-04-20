package com.combotto.controlplane.repositories;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.AssetEntity;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
  Optional<AssetEntity> findByIdAndTenantId(UUID id, String tenantId);
  boolean existsByIdAndTenantId(UUID id, String tenantId);
  Page<AssetEntity> findAllByTenantId(String tenantId, Pageable pageable);
}
