package com.combotto.controlplane.mappers;

import org.springframework.stereotype.Component;

import com.combotto.controlplane.api.CertificateRenewalHistoryResponse;
import com.combotto.controlplane.model.CertificateRenewalStatusHistoryEntity;

@Component
public class CertificateRenewalHistoryMapper {

    public CertificateRenewalHistoryResponse toResponse(CertificateRenewalStatusHistoryEntity entity) {
        return new CertificateRenewalHistoryResponse(
                entity.getId(),
                entity.getCertificateId(),
                entity.getTenantId(),
                entity.getOldRenewalStatus(),
                entity.getNewRenewalStatus(),
                entity.getBlockedReason(),
                entity.getUpdatedBy(),
                entity.getOccurredAt(),
                entity.getCreatedAt());
    }
}