package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;
import com.combotto.controlplane.repositories.OutboxEventRepository;

@Component
public class OutboxPublisher {

  private final OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;

  public OutboxPublisher(
      OutboxEventRepository outboxEventRepository,
      KafkaTemplate<String, String> kafkaTemplate) {
    this.outboxEventRepository = outboxEventRepository;
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publishPendingEvents() {
    List<OutboxEventEntity> pendingEvents = outboxEventRepository
        .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

    for (

    OutboxEventEntity event : pendingEvents) {
      kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload());
      event.setStatus(OutboxStatus.PUBLISHED);
      event.setPublishedAt(OffsetDateTime.now());
      outboxEventRepository.save(event);
    }
  }
}
