package com.combotto.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.CertificateBindingEntity;

public interface CertificateBindingRepository extends JpaRepository<CertificateBindingEntity, UUID> {
  List<CertificateBindingEntity> findByAssetId(UUID assetId);
  Page<CertificateBindingEntity> findByAssetId(UUID assetId, Pageable pageable);
  List<CertificateBindingEntity> findByCertificateId(UUID certificateId);
  Page<CertificateBindingEntity> findByCertificateId(UUID certificateId, Pageable pageable);
  List<CertificateBindingEntity> findByAssetIdAndAssetTenantId(UUID assetId, String tenantId);
  Page<CertificateBindingEntity> findByAssetIdAndAssetTenantId(UUID assetId, String tenantId, Pageable pageable);
  List<CertificateBindingEntity> findByCertificateIdAndCertificateTenantId(UUID certificateId, String tenantId);
  Page<CertificateBindingEntity> findByCertificateIdAndCertificateTenantId(
      UUID certificateId, String tenantId, Pageable pageable);
}
