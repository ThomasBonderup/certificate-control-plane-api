package com.combotto.controlplane.services;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.combotto.controlplane.model.EvidenceEnvelope;

@Component
public class KafkaEvidencePublisher implements EvidencePublisher {

  private final KafkaTemplate<String, EvidenceEnvelope> kafkaTemplate;
  private final String topic;

  public KafkaEvidencePublisher(
    KafkaTemplate<String, EvidenceEnvelope> kafkaTemplate,
    @Value("${audit.kafka.topics.evidence-raw:evidence.raw}") String topic
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  public void publish(EvidenceEnvelope envelope) {
    String key = envelope.assetId();

    var msg = MessageBuilder.withPayload(envelope)
      .setHeader(KafkaHeaders.TOPIC, topic)
      .setHeader(KafkaHeaders.KEY, key)
      .setHeader("eventId", String.valueOf(envelope.id()).getBytes(StandardCharsets.UTF_8))
      .build();

    kafkaTemplate.send(msg);
  }
}
