package com.combotto.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.combotto.controlplane.model.CertificateRenewalStatusHistoryEntity;

public interface CertificateRenewalStatusHistoryRepository
        extends JpaRepository<CertificateRenewalStatusHistoryEntity, UUID> {
    List<CertificateRenewalStatusHistoryEntity> findByCertificateIdOrderByOccurredAtDesc(UUID certificateId);
}
