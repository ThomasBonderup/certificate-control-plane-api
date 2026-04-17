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
}
