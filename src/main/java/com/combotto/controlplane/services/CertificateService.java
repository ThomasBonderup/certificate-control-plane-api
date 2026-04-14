package com.combotto.controlplane.services;

import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.repositories.CertificateRepository;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List

@Service
public class CertificateService {
  private final CertificateRepository certificateRepository;

  public CertificateService(CertificateRepository certificateRepository) {
    this.certificateRepository = certificateRepository;
  }

  public CertificateResponse create(CreateCertificateRequest request) {
    OffsetDateTime now = OffsetDateTime.now();

    CertificateEntity entity = new CertificateEntity();
    entity.setId(UUID.randomUUID());
    entity.setTenantId(request.tenantId());
    entity.setName(request.name());
    entity.setCommonName(request.commonName());
    entity.setIssuer(request.issuer());
    entity.setSerialNumber(request.serialNumber());
    entity.setSha256Fingerprint(request.sha256Fingerprint());
    entity.setNotBefore(request.notBefore());
    entity.setNotAfter(request.notAfter());
    entity.setStatus(request.status());
    entity.setRenewalStatus(request.renewalStatus());
    entity.setOwner(request.owner());
    entity.setNotes(request.notes());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    return toResponse(certificateRepository.save(entity));
  }

  public List<CertificateResponse> list() {
    return certificateRepository.findAll()
      .stream()
      .map(this::toResponse)
      .toList();
  }

  public CertificateResponse getById(UUID id) {
    CertificateEntity entity = certificateRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id));
    return toResponse(entity);
  }

  public CertificateResponse update(UUID id, UpdateCertificateRequest request) {
    CertificateEntity entity = certificateRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id));

    if (request.name() != null) entity.setName(request.name());
    if (request.commonName() != null) entity.setCommonName(request.commonName());
    if (request.issuer() != null) entity.setIssuer(request.issuer());
    if (request.serialNumber() != null) entity.setSerialNumber(request.serialNumber());
    if (request.sha256Fingerprint() != null) entity.setSha256Fingerprint(request.sha256Fingerprint());
    if (request.notBefore() != null) entity.setNotBefore(request.notBefore());
    if (request.notAfter() != null) entity.setNotAfter(request.notAfter());
    if (request.status() != null) entity.setStatus(request.status());
    if (request.renewalStatus() != null) entity.setRenewalStatus(request.renewalStatus());
    if (request.owner() != null) entity.setOwner(request.owner());
    if (request.notes() != null) entity.setNotes(request.notes());

    entity.setUpdatedAt(OffsetDateTime.now());

    return toResponse(certificateRepository.save(entity));
  }

  public void delete(UUID id) {
    if (!certificateRepository.existsById(id)) {
      throw new ResourceNotFoundException("Certificate not found: " + id);
    }
    certificateRepository.deleteById(id);
  }

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
        entity.getUpdatedAt());
  }
}
