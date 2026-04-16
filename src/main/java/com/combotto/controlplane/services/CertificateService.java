package com.combotto.controlplane.services;

import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CertificateSummaryResponse;
import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.common.CertificateMapper;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateRepository;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

@Service
public class CertificateService {
  private final CertificateRepository certificateRepository;
  private final CertificateMapper certificateMapper;

  public CertificateService(CertificateRepository certificateRepository, CertificateMapper certificateMapper) {
    this.certificateRepository = certificateRepository;
    this.certificateMapper = certificateMapper;
  }

  public CertificateResponse create(CreateCertificateRequest request) {
    OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);

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

    return certificateMapper.toResponse(certificateRepository.save(entity));
  }

  public List<CertificateResponse> list(
      String tenantId,
      CertificateStatus status,
      RenewalStatus renewalStatus) {

    String normalizedTenantId = normalize(tenantId);

    return certificateRepository.findByFilters(normalizedTenantId, status, renewalStatus)
        .stream()
        .map(certificateMapper::toResponse)
        .toList();
  }

  public List<CertificateResponse> listExpiringSoon(int days) {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime threshold = now.plusDays(days);

    return certificateRepository.findExpiringSoon(now, threshold)
        .stream()
        .map(certificateMapper::toResponse)
        .toList();
  }

  public CertificateResponse getById(UUID id) {
    CertificateEntity entity = certificateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id));
    return certificateMapper.toResponse(entity);
  }

  public CertificateResponse update(UUID id, UpdateCertificateRequest request) {
    CertificateEntity entity = certificateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id));

    if (request.name() != null)
      entity.setName(request.name());
    if (request.commonName() != null)
      entity.setCommonName(request.commonName());
    if (request.issuer() != null)
      entity.setIssuer(request.issuer());
    if (request.serialNumber() != null)
      entity.setSerialNumber(request.serialNumber());
    if (request.sha256Fingerprint() != null)
      entity.setSha256Fingerprint(request.sha256Fingerprint());
    if (request.notBefore() != null)
      entity.setNotBefore(request.notBefore());
    if (request.notAfter() != null)
      entity.setNotAfter(request.notAfter());
    if (request.status() != null)
      entity.setStatus(request.status());
    if (request.renewalStatus() != null)
      entity.setRenewalStatus(request.renewalStatus());
    if (request.owner() != null)
      entity.setOwner(request.owner());
    if (request.notes() != null)
      entity.setNotes(request.notes());

    entity.setUpdatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));

    return certificateMapper.toResponse(certificateRepository.save(entity));
  }

  public void delete(UUID id) {
    if (!certificateRepository.existsById(id)) {
      throw new ResourceNotFoundException("Certificate not found: " + id);
    }
    certificateRepository.deleteById(id);
  }

  public CertificateSummaryResponse summary() {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime soon = now.plusDays(30);

    return new CertificateSummaryResponse(
        certificateRepository.count(),
        certificateRepository.countByStatus(CertificateStatus.ACTIVE),
        certificateRepository.countExpiringSoon(now, soon),
        certificateRepository.countByStatus(CertificateStatus.EXPIRED),
        certificateRepository.countByRenewalStatus(RenewalStatus.IN_PROGRESS));
  }

  private String normalize(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
