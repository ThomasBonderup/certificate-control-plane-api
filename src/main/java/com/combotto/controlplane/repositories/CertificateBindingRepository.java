package com.combotto.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.combotto.controlplane.model.CertificateBindingEntity;

public interface CertificateBindingRepository extends JpaRepository<CertificateBindingEntity, UUID> {
  List<CertificateBindingEntity> findByAssetId(Long assetId);
  Page<CertificateBindingEntity> findByAssetId(Long assetId, Pageable pageable);
  List<CertificateBindingEntity> findByCertificateId(UUID certificateId);
  Page<CertificateBindingEntity> findByCertificateId(UUID certificateId, Pageable pageable);
  List<CertificateBindingEntity> findByAssetIdAndCertificateTenantId(Long assetId, String tenantId);
  Page<CertificateBindingEntity> findByAssetIdAndCertificateTenantId(Long assetId, String tenantId, Pageable pageable);
  List<CertificateBindingEntity> findByCertificateIdAndCertificateTenantId(UUID certificateId, String tenantId);
  Page<CertificateBindingEntity> findByCertificateIdAndCertificateTenantId(
      UUID certificateId, String tenantId, Pageable pageable);

  @Query("""
      select b.bindingType as bindingType, count(b) as count
      from CertificateBindingEntity b
      group by b.bindingType
      """)
  List<CertificateBindingTypeCount> countByBindingType();
}
