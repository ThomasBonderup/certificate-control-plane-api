package com.combotto.controlplane.common;

import org.springframework.stereotype.Component;

import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.model.CertificateEntity;

@Component
public class CertificateMapper {

  public CertificateResponse toResponse(CertificateEntity entity) {
    return new CertificateResponse(
        entity.getId(),
        entity.getTenantId(),
        entity.getName(),
        entity.getCommonName(),
        entity.getIssuer(),
        entity.getSerialNumber(),
        entity.getSha256Fingerprint(),
        entity.getNotBefore(),
        entity.getNotAfter(),
        entity.getStatus().name(),
        entity.getRenewalStatus().name(),
        entity.getOwner(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getCreatedBy(),
        entity.getUpdatedBy());
  };

}
