package com.combotto.controlplane.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;

@Component
public class CertificateEventPublisher {

  private final KafkaTemplate<String, CertificateRenewalStatusChangedEvent> kafkaTemplate;
  private final String topic;

  public CertificateEventPublisher(
      KafkaTemplate<String, CertificateRenewalStatusChangedEvent> kafkaTemplate,
      @Value("${audit.kafka.topics.certificate.renew-status-changed:certificate-renewal-status-changed}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void publishRenewalStatusChanged(CertificateRenewalStatusChangedEvent event) {
    kafkaTemplate.send(topic, event.certificateId().toString(), event);
  }

}
