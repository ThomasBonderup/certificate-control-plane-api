package com.combotto.controlplane.services;

import java.util.List;
import java.util.UUID;

import com.combotto.controlplane.api.CertificateRenewalHistoryResponse;
import com.combotto.controlplane.common.CurrentTenantProvider;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.common.TenantAccessValidator;
import com.combotto.controlplane.mappers.CertificateRenewalHistoryMapper;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.repositories.CertificateRenewalStatusHistoryRepository;
import com.combotto.controlplane.repositories.CertificateRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CertificateRenewalStatusHistoryService {

  private final CertificateRepository certificateRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final TenantAccessValidator tenantAccessValidator;
  private final CertificateRenewalStatusHistoryRepository certificateRenewalStatusHistoryRepository;
  private final CertificateRenewalHistoryMapper certificateRenewalHistoryMapper;

  public CertificateRenewalStatusHistoryService(
      CertificateRepository certificateRepository,
      CurrentTenantProvider currentTenantProvider,
      TenantAccessValidator tenantAccessValidator,
      CertificateRenewalStatusHistoryRepository certificateRenewalStatusHistoryRepository,
      CertificateRenewalHistoryMapper certificateRenewalHistoryMapper) {
    this.certificateRepository = certificateRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.tenantAccessValidator = tenantAccessValidator;
    this.certificateRenewalStatusHistoryRepository = certificateRenewalStatusHistoryRepository;
    this.certificateRenewalHistoryMapper = certificateRenewalHistoryMapper;
  }

  public List<CertificateRenewalHistoryResponse> listByCertificateId(UUID certificateId) {
    CertificateEntity certificate = certificateRepository.findById(certificateId)
        .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

    String currentTenantId = currentTenantProvider.getRequiredTenantId();
    if (!certificate.getTenantId().equals(currentTenantId)) {
      throw new AccessDeniedException("Access denied");
    }

    return certificateRenewalStatusHistoryRepository.findByCertificateIdOrderByOccurredAtDesc(certificateId)
        .stream()
        .map(certificateRenewalHistoryMapper::toResponse)
        .toList();
  }
}
