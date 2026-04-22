package com.combotto.controlplane.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.model.CertificateRenewalStatusHistoryEntity;
import com.combotto.controlplane.repositories.CertificateRenewalStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
class CertificateRenewalStatusChangedConsumerTest {

  @Mock
  private CertificateRenewalStatusHistoryRepository historyRepository;

  @InjectMocks
  private CertificateRenewalStatusChangedConsumer consumer;

  @Test
  void handleSavesMappedHistoryRow() {
    UUID certificateId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.parse("2026-04-22T10:00:00Z");
    OffsetDateTime renewalUpdatedAt = OffsetDateTime.parse("2026-04-22T10:00:00Z");
    CertificateRenewalStatusChangedEvent event =
        new CertificateRenewalStatusChangedEvent(
            certificateId,
            "demo-tenant",
            "PLANNED",
            "BLOCKED",
            "Waiting on vendor",
            renewalUpdatedAt,
            "test-user",
            occurredAt);

    ArgumentCaptor<CertificateRenewalStatusHistoryEntity> captor =
        ArgumentCaptor.forClass(CertificateRenewalStatusHistoryEntity.class);

    consumer.handle(event);

    verify(historyRepository).save(captor.capture());

    CertificateRenewalStatusHistoryEntity saved = captor.getValue();
    assertNotNull(saved.getId());
    assertEquals(certificateId, saved.getCertificateId());
    assertEquals("demo-tenant", saved.getTenantId());
    assertEquals("PLANNED", saved.getOldRenewalStatus());
    assertEquals("BLOCKED", saved.getNewRenewalStatus());
    assertEquals("Waiting on vendor", saved.getBlockedReason());
    assertEquals("test-user", saved.getUpdatedBy());
    assertEquals(occurredAt, saved.getOccurredAt());
    assertNotNull(saved.getCreatedAt());
  }

  @Test
  void handlePreservesNullOptionalFields() {
    UUID certificateId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.parse("2026-04-22T11:00:00Z");
    CertificateRenewalStatusChangedEvent event =
        new CertificateRenewalStatusChangedEvent(
            certificateId,
            "demo-tenant",
            null,
            "PLANNED",
            null,
            OffsetDateTime.parse("2026-04-22T11:00:00Z"),
            null,
            occurredAt);

    ArgumentCaptor<CertificateRenewalStatusHistoryEntity> captor =
        ArgumentCaptor.forClass(CertificateRenewalStatusHistoryEntity.class);

    consumer.handle(event);

    verify(historyRepository).save(captor.capture());

    CertificateRenewalStatusHistoryEntity saved = captor.getValue();
    assertEquals(certificateId, saved.getCertificateId());
    assertNull(saved.getOldRenewalStatus());
    assertEquals("PLANNED", saved.getNewRenewalStatus());
    assertNull(saved.getBlockedReason());
    assertNull(saved.getUpdatedBy());
    assertEquals(occurredAt, saved.getOccurredAt());
  }
}
