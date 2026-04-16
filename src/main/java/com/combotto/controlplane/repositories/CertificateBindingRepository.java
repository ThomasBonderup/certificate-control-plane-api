package com.combotto.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.CertificateBindingEntity;

public interface CertificateBindingRepository extends JpaRepository<CertificateBindingEntity, UUID> {
  List<CertificateBindingEntity> findByCertificateId(UUID certificateId);
}
