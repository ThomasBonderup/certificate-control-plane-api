package com.combotto.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import com.combotto.controlplane.setup.KafkaProducerConfig;

@SpringJUnitConfig(KafkaProducerConfig.class)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=test-broker:19092")
class KafkaProducerConfigTest {

  @Autowired
  @Qualifier("evidenceProducerFactory")
  ProducerFactory<?, ?> producerFactory;

  @Test
  void producerFactoryUsesConfiguredBootstrapServers() {
    var defaultProducerFactory = (DefaultKafkaProducerFactory<?, ?>) producerFactory;
    Map<String, Object> configs = defaultProducerFactory.getConfigurationProperties();

    assertThat(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
        .isEqualTo("test-broker:19092");
  }
}
