package com.combotto.controlplane.services;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CertificateSummaryResponse;
import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.common.BadRequestException;
import com.combotto.controlplane.common.CertificateMapper;
import com.combotto.controlplane.common.CurrentTenantProvider;
import com.combotto.controlplane.common.CurrentUserProvider;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.common.TenantAccessValidator;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateRepository;
import com.combotto.controlplane.repositories.OutboxEventRepository;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CertificateService {
  private final CertificateRepository certificateRepository;
  private final CertificateMapper certificateMapper;
  private final CurrentUserProvider currentUserProvider;
  private final CurrentTenantProvider currentTenantProvider;
  private final TenantAccessValidator tenantAccessValidator;
  private final MeterRegistry meterRegistry;
  private final OutboxEventWriter outboxEventWriter;

  public CertificateService(
      CertificateRepository certificateRepository,
      CertificateMapper certificateMapper,
      CurrentUserProvider currentUserProvider,
      CurrentTenantProvider currentTenantProvider,
      TenantAccessValidator tenantAccessValidator,
      MeterRegistry meterRegistry,
      OutboxEventWriter outboxEventWriter) {
    this.certificateRepository = certificateRepository;
    this.certificateMapper = certificateMapper;
    this.currentUserProvider = currentUserProvider;
    this.currentTenantProvider = currentTenantProvider;
    this.tenantAccessValidator = tenantAccessValidator;
    this.meterRegistry = meterRegistry;
    this.outboxEventWriter = outboxEventWriter;
  }

  public CertificateResponse create(CreateCertificateRequest request) {
    String tenantId = currentTenantProvider.getRequiredTenantId();
    tenantAccessValidator.validateTenantMatch(request.tenantId(), tenantId);

    OffsetDateTime now = currentTimestamp();

    CertificateEntity entity = new CertificateEntity();
    entity.setId(UUID.randomUUID());
    entity.setTenantId(tenantId);
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

    String currentUser = currentUserProvider.getCurrentUserId();
    entity.setCreatedBy(currentUser);
    entity.setUpdatedBy(currentUser);

    return certificateMapper.toResponse(certificateRepository.save(entity));
  }

  public Page<CertificateResponse> list(
      String tenantId,
      CertificateStatus status,
      RenewalStatus renewalStatus,
      Pageable pageable) {
    String claimTenantId = currentTenantProvider.getRequiredTenantId();
    String normalizedTenantId = normalize(tenantId);
    tenantAccessValidator.validateTenantMatch(normalizedTenantId, claimTenantId);

    return certificateRepository.findByFilters(claimTenantId, status, renewalStatus, pageable)
        .map(certificateMapper::toResponse);
  }

  public Page<CertificateResponse> listExpiringSoon(
      int days,
      String tenantId,
      String owner,
      RenewalStatus renewalStatus,
      Pageable pageable) {
    String claimTenantId = currentTenantProvider.getRequiredTenantId();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime threshold = now.plusDays(days);
    String normalizedTenantId = normalize(tenantId);
    tenantAccessValidator.validateTenantMatch(normalizedTenantId, claimTenantId);
    String normalizedOwner = normalize(owner);

    return certificateRepository.findExpiringSoonByFilters(
        now,
        threshold,
        claimTenantId,
        normalizedOwner,
        renewalStatus,
        pageable)
        .map(certificateMapper::toResponse);
  }

  public Page<CertificateResponse> listAttentionNeeded(int days, Pageable pageable) {
    String tenantId = currentTenantProvider.getRequiredTenantId();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime threshold = now.plusDays(days);

    return certificateRepository.findAttentionNeeded(
        now,
        threshold,
        tenantId,
        RenewalStatus.NOT_STATUS,
        RenewalStatus.PLANNED,
        RenewalStatus.IN_PROGRESS,
        RenewalStatus.BLOCKED,
        pageable)
        .map(certificateMapper::toResponse);
  }

  public CertificateResponse getById(UUID id) {
    CertificateEntity entity = findCertificate(id);
    return certificateMapper.toResponse(entity);
  }

  @Transactional
  public CertificateResponse update(UUID id, UpdateCertificateRequest request) {
    CertificateEntity entity = findCertificate(id);
    OffsetDateTime now = currentTimestamp();
    RenewalStatus previousRenewalStatus = entity.getRenewalStatus();

    applyMetadataUpdates(request, entity);
    applyRenewalWorkflow(request, entity, now);
    stampAuditFields(entity, now);

    CertificateEntity savedCertificateEntity = certificateRepository.save(entity);

    publishRenewalStatusChangedOutboxIfNeeded(savedCertificateEntity, previousRenewalStatus, now);

    return certificateMapper.toResponse(savedCertificateEntity);
  }

  private void publishRenewalStatusChangedOutboxIfNeeded(
      CertificateEntity saved,
      RenewalStatus previousRenewalStatus,
      OffsetDateTime occurredAt) {
    RenewalStatus currentRenewalStatus = saved.getRenewalStatus();

    if (saved.getRenewalStatus() == null || currentRenewalStatus == previousRenewalStatus) {
      return;
    }

    CertificateRenewalStatusChangedEvent event = new CertificateRenewalStatusChangedEvent(
        saved.getId(),
        saved.getTenantId(),
        previousRenewalStatus != null ? previousRenewalStatus.name() : null,
        currentRenewalStatus.name(),
        saved.getBlockedReason(),
        saved.getRenewalUpdatedAt(),
        saved.getUpdatedBy(),
        occurredAt);

    outboxEventWriter.appendRenewalStatusChanged(event);
  }

  private void applyMetadataUpdates(UpdateCertificateRequest request, CertificateEntity entity) {
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
    if (request.owner() != null)
      entity.setOwner(request.owner());
    if (request.notes() != null)
      entity.setNotes(request.notes());
  }

  private void applyRenewalWorkflow(UpdateCertificateRequest request, CertificateEntity entity, OffsetDateTime now) {

    if (request.renewalStatus() == null) {
      return;
    }

    RenewalStatus newStatus = request.renewalStatus();
    RenewalStatus currentStatus = entity.getRenewalStatus();

    if (newStatus != currentStatus) {
      entity.setRenewalStatus(newStatus);
      entity.setRenewalUpdatedAt(now);
      meterRegistry.counter(
          "combotto.certificate.renewal.status.change",
          "status", entity.getRenewalStatus().name()).increment();
    }

    if (newStatus == RenewalStatus.BLOCKED) {
      validateBlockedReason(request.blockedReason());
      entity.setBlockedReason(request.blockedReason());
      meterRegistry.counter("combotto.certificate.renewal.blocked").increment();
    } else {
      entity.setBlockedReason(null);
    }
  }

  private void validateBlockedReason(String blockedReason) {
    if (blockedReason == null || blockedReason.isBlank()) {
      throw new BadRequestException("blockedReason is required when renewalStatus is BLOCKED");
    }
  }

  private OffsetDateTime currentTimestamp() {
    return OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
  }

  private CertificateEntity findCertificate(UUID id) {
    return certificateRepository.findByIdAndTenantId(id, currentTenantProvider.getRequiredTenantId())
        .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id));
  }

  private void stampAuditFields(CertificateEntity entity, OffsetDateTime now) {
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(currentUserProvider.getCurrentUserId());
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID id) {
    if (!certificateRepository.existsByIdAndTenantId(id, currentTenantProvider.getRequiredTenantId())) {
      throw new ResourceNotFoundException("Certificate not found: " + id);
    }
    certificateRepository.deleteById(id);
  }

  public CertificateSummaryResponse summary() {
    String tenantId = currentTenantProvider.getRequiredTenantId();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime soon = now.plusDays(30);

    return new CertificateSummaryResponse(
        certificateRepository.countByTenantId(tenantId),
        certificateRepository.countByTenantIdAndStatus(tenantId, CertificateStatus.ACTIVE),
        certificateRepository.countExpiringSoon(now, tenantId, soon),
        certificateRepository.countByTenantIdAndStatus(tenantId, CertificateStatus.EXPIRED),
        certificateRepository.countByTenantIdAndRenewalStatus(tenantId, RenewalStatus.IN_PROGRESS));
  }

  private String normalize(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
