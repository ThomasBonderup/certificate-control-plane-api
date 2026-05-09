package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateBindingRepository;
import com.combotto.controlplane.repositories.CertificateRepository;

@Service
public class CertificatePostureService {

  private static final List<Integer> EXPIRY_WINDOWS_DAYS = List.of(30, 60, 90);

  private final CertificateRepository certificateRepository;
  private final CertificateBindingRepository certificateBindingRepository;

  public CertificatePostureService(
      CertificateRepository certificateRepository,
      CertificateBindingRepository certificateBindingRepository) {
    this.certificateRepository = certificateRepository;
    this.certificateBindingRepository = certificateBindingRepository;
  }

  public CertificatePostureSnapshot snapshot() {
    OffsetDateTime now = OffsetDateTime.now();

    Map<CertificateStatusRenewalKey, Long> certificateCounts = new LinkedHashMap<>();
    certificateRepository.countByStatusAndRenewalStatus()
        .forEach(count -> certificateCounts.put(
            new CertificateStatusRenewalKey(
                count.getStatus().name(),
                count.getRenewalStatus().name()),
            count.getCount()));

    Map<Integer, Long> expiringCountsByWindowDays = new LinkedHashMap<>();
    for (Integer days : EXPIRY_WINDOWS_DAYS) {
      expiringCountsByWindowDays.put(days, certificateRepository.countExpiringSoon(now, now.plusDays(days)));
    }

    Map<String, Long> bindingCountsByType = new LinkedHashMap<>();
    certificateBindingRepository.countByBindingType()
        .forEach(count -> bindingCountsByType.put(count.getBindingType().name(), count.getCount()));

    return new CertificatePostureSnapshot(
        certificateCounts,
        expiringCountsByWindowDays,
        certificateRepository.countByStatus(CertificateStatus.EXPIRED),
        certificateRepository.countByRenewalStatus(RenewalStatus.BLOCKED),
        certificateRepository.countUnbound(),
        bindingCountsByType,
        certificateRepository.findNextExpiry());
  }
}
