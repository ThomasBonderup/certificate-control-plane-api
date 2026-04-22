package com.combotto.controlplane.setup;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import com.combotto.controlplane.api.CertificateRenewalStatusChangedEvent;
import com.combotto.controlplane.model.EvidenceEnvelope;

@Configuration
public class KafkaProducerConfig {
  
  @Bean
  public ProducerFactory<String, EvidenceEnvelope> evidenceProducerFactory(
      @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public ProducerFactory<String, CertificateRenewalStatusChangedEvent> certificateRenewalStatusChangedProducerFactory(
      @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public ProducerFactory<String, String> outboxProducerFactory(
      @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(cfg);
  }

  @Bean
  public KafkaTemplate<String, EvidenceEnvelope> kafkaTemplate(
    ProducerFactory<String, EvidenceEnvelope> evidenceProducerFactory
  ) {
    return new KafkaTemplate<>(evidenceProducerFactory);
  }

  @Bean
  public KafkaTemplate<String, CertificateRenewalStatusChangedEvent> certificateRenewalStatusChangedKafkaTemplate(
      ProducerFactory<String, CertificateRenewalStatusChangedEvent> certificateRenewalStatusChangedProducerFactory) {
    return new KafkaTemplate<>(certificateRenewalStatusChangedProducerFactory);
  }

  @Bean
  public KafkaTemplate<String, String> outboxKafkaTemplate(
      ProducerFactory<String, String> outboxProducerFactory) {
    return new KafkaTemplate<>(outboxProducerFactory);
  }
}
