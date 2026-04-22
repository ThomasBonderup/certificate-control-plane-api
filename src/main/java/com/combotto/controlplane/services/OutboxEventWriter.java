package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;
import com.combotto.controlplane.repositories.OutboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxEventWriter {

  private static final String CERTIFICATE_AGGREGATE_TYPE = "CERTIFICATE";
  private static final String RENEWAL_STATUS_CHANGED_EVENT_TYPE = "certificate.renewal-status-changed";
  private static final String RENEWAL_STATUS_CHANGED_TOPIC = "certificate.renewal-status-changed";

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public OutboxEventWriter(
      OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  public void appendRenewalStatusChanged(CertificateRenewalStatusChangedEvent event) {
    OutboxEventEntity outboxEvent = new OutboxEventEntity();

    outboxEvent.setId(UUID.randomUUID());
    outboxEvent.setAggregateType(CERTIFICATE_AGGREGATE_TYPE);
    outboxEvent.setAggregateId(event.certificateId());
    outboxEvent.setEventType(RENEWAL_STATUS_CHANGED_EVENT_TYPE);
    outboxEvent.setTopic(RENEWAL_STATUS_CHANGED_TOPIC);
    outboxEvent.setEventKey(event.certificateId().toString());
    outboxEvent.setPayload(serialize(event));
    outboxEvent.setCreatedAt(currentTimestamp());
    outboxEvent.setStatus(OutboxStatus.PENDING);

    outboxEventRepository.save(outboxEvent);
  }

  private String serialize(CertificateRenewalStatusChangedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize renewal status changed event", e);
    }
  }

  private OffsetDateTime currentTimestamp() {
    return OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
  }

}
