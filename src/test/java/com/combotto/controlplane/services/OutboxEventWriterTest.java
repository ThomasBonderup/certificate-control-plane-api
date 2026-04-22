package com.combotto.controlplane.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;
import com.combotto.controlplane.repositories.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventWriterTest {

  @Mock
  private OutboxEventRepository outboxEventRepository;

  private OutboxEventWriter outboxEventWriter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = JsonMapper.builder().findAndAddModules().build();
    outboxEventWriter = new OutboxEventWriter(outboxEventRepository, objectMapper);

    when(outboxEventRepository.save(any(OutboxEventEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void appendRenewalStatusChangedPersistsPendingOutboxRecord() throws Exception {
    UUID certificateId = UUID.randomUUID();
    OffsetDateTime renewalUpdatedAt = OffsetDateTime.parse("2026-04-22T08:15:30Z");
    OffsetDateTime occurredAt = OffsetDateTime.parse("2026-04-22T08:16:00Z");
    CertificateRenewalStatusChangedEvent event = new CertificateRenewalStatusChangedEvent(
        certificateId,
        "demo-tenant",
        "PLANNED",
        "BLOCKED",
        "Waiting on vendor",
        renewalUpdatedAt,
        "test-user",
        occurredAt);

    ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);

    outboxEventWriter.appendRenewalStatusChanged(event);

    verify(outboxEventRepository).save(captor.capture());

    OutboxEventEntity saved = captor.getValue();
    assertNotNull(saved.getId());
    assertEquals("CERTIFICATE", saved.getAggregateType());
    assertEquals(certificateId, saved.getAggregateId());
    assertEquals("certificate.renewal-status-changed", saved.getEventType());
    assertEquals("certificate.renewal-status-changed", saved.getTopic());
    assertEquals(certificateId.toString(), saved.getEventKey());
    assertEquals(OutboxStatus.PENDING, saved.getStatus());
    assertNotNull(saved.getCreatedAt());
    assertNull(saved.getPublishedAt());

    CertificateRenewalStatusChangedEvent serialized = objectMapper.readValue(
        saved.getPayload(),
        CertificateRenewalStatusChangedEvent.class);
    assertEquals(event, serialized);
  }
}
