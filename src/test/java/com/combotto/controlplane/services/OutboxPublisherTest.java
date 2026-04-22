package com.combotto.controlplane.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.combotto.controlplane.model.OutboxEventEntity;
import com.combotto.controlplane.model.OutboxStatus;
import com.combotto.controlplane.repositories.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  @Mock
  private OutboxEventRepository outboxEventRepository;

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  private OutboxPublisher outboxPublisher;

  @BeforeEach
  void setUp() {
    outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);
  }

  @Test
  void publishPendingEventsPublishesAndMarksEventsAsPublished() {
    OutboxEventEntity first = pendingEvent("topic-a", "key-a", "{\"id\":1}");
    OutboxEventEntity second = pendingEvent("topic-b", "key-b", "{\"id\":2}");
    when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(first, second));

    outboxPublisher.publishPendingEvents();

    InOrder inOrder = inOrder(kafkaTemplate, outboxEventRepository);
    inOrder.verify(kafkaTemplate).send("topic-a", "key-a", "{\"id\":1}");
    inOrder.verify(outboxEventRepository).save(first);
    inOrder.verify(kafkaTemplate).send("topic-b", "key-b", "{\"id\":2}");
    inOrder.verify(outboxEventRepository).save(second);

    assertEquals(OutboxStatus.PUBLISHED, first.getStatus());
    assertEquals(OutboxStatus.PUBLISHED, second.getStatus());
    assertNotNull(first.getPublishedAt());
    assertNotNull(second.getPublishedAt());
  }

  @Test
  void publishPendingEventsDoesNothingWhenNoPendingEventsExist() {
    when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of());

    outboxPublisher.publishPendingEvents();

    verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString());
    verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any(OutboxEventEntity.class));
  }

  private OutboxEventEntity pendingEvent(String topic, String eventKey, String payload) {
    OutboxEventEntity event = new OutboxEventEntity();
    event.setId(UUID.randomUUID());
    event.setAggregateType("CERTIFICATE");
    event.setAggregateId(UUID.randomUUID());
    event.setEventType("certificate.renewal-status-changed");
    event.setTopic(topic);
    event.setEventKey(eventKey);
    event.setPayload(payload);
    event.setCreatedAt(OffsetDateTime.parse("2026-04-22T08:00:00Z"));
    event.setStatus(OutboxStatus.PENDING);
    return event;
  }
}
