package com.combotto.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.combotto.controlplane.repositories.CertificateRepository;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=test-broker:19092",
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class KafkaProducerConfigTest {

  @Autowired
  ProducerFactory<?, ?> producerFactory;

  @MockitoBean
  CertificateRepository certificateRepository;

  @Test
  void producerFactoryUsesConfiguredBootstrapServers() {
    var defaultProducerFactory = (DefaultKafkaProducerFactory<?, ?>) producerFactory;
    Map<String, Object> configs = defaultProducerFactory.getConfigurationProperties();

    assertThat(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
        .isEqualTo("test-broker:19092");
  }
}
