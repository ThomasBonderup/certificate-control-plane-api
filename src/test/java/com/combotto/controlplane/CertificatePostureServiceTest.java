package com.combotto.controlplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.combotto.controlplane.model.BindingType;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateBindingRepository;
import com.combotto.controlplane.repositories.CertificateBindingTypeCount;
import com.combotto.controlplane.repositories.CertificateRepository;
import com.combotto.controlplane.repositories.CertificateStatusRenewalCount;
import com.combotto.controlplane.services.CertificatePostureService;
import com.combotto.controlplane.services.CertificatePostureSnapshot;
import com.combotto.controlplane.services.CertificateStatusRenewalKey;

@ExtendWith(MockitoExtension.class)
class CertificatePostureServiceTest {

  @Mock
  private CertificateRepository certificateRepository;

  @Mock
  private CertificateBindingRepository certificateBindingRepository;

  private CertificatePostureService postureService;

  @BeforeEach
  void setUp() {
    postureService = new CertificatePostureService(certificateRepository, certificateBindingRepository);
  }

  @Test
  void snapshotAggregatesCertificatePosture() {
    OffsetDateTime nextExpiry = OffsetDateTime.parse("2027-12-16T13:55:40Z");

    when(certificateRepository.countByStatusAndRenewalStatus())
        .thenReturn(List.of(
            certificateCount(CertificateStatus.ACTIVE, RenewalStatus.NOT_STATUS, 6),
            certificateCount(CertificateStatus.EXPIRING_SOON, RenewalStatus.BLOCKED, 1)));
    when(certificateRepository.countExpiringSoon(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(0L, 1L, 2L);
    when(certificateRepository.countByStatus(CertificateStatus.EXPIRED)).thenReturn(3L);
    when(certificateRepository.countByRenewalStatus(RenewalStatus.BLOCKED)).thenReturn(1L);
    when(certificateRepository.countUnbound()).thenReturn(1L);
    when(certificateRepository.findNextExpiry()).thenReturn(nextExpiry);
    when(certificateBindingRepository.countByBindingType())
        .thenReturn(List.of(
            bindingCount(BindingType.MQTT_DEVICE_CLIENT_AUTH, 3),
            bindingCount(BindingType.TRUST_BUNDLE, 2)));

    CertificatePostureSnapshot snapshot = postureService.snapshot();

    assertEquals(6L, snapshot.certificateCounts()
        .get(new CertificateStatusRenewalKey("ACTIVE", "NOT_STATUS")));
    assertEquals(1L, snapshot.certificateCounts()
        .get(new CertificateStatusRenewalKey("EXPIRING_SOON", "BLOCKED")));
    assertEquals(0L, snapshot.expiringCountsByWindowDays().get(30));
    assertEquals(1L, snapshot.expiringCountsByWindowDays().get(60));
    assertEquals(2L, snapshot.expiringCountsByWindowDays().get(90));
    assertEquals(3L, snapshot.expiredCount());
    assertEquals(1L, snapshot.blockedRenewalCount());
    assertEquals(1L, snapshot.unboundCount());
    assertEquals(3L, snapshot.bindingCountsByType().get("MQTT_DEVICE_CLIENT_AUTH"));
    assertEquals(2L, snapshot.bindingCountsByType().get("TRUST_BUNDLE"));
    assertEquals(nextExpiry, snapshot.nextExpiry());
  }

  private CertificateStatusRenewalCount certificateCount(
      CertificateStatus status,
      RenewalStatus renewalStatus,
      long count) {
    return new CertificateStatusRenewalCount() {
      @Override
      public CertificateStatus getStatus() {
        return status;
      }

      @Override
      public RenewalStatus getRenewalStatus() {
        return renewalStatus;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }

  private CertificateBindingTypeCount bindingCount(BindingType bindingType, long count) {
    return new CertificateBindingTypeCount() {
      @Override
      public BindingType getBindingType() {
        return bindingType;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }
}
