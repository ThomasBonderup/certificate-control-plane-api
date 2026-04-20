package com.combotto.controlplane;

import static com.combotto.controlplane.support.SecurityTestSupport.readOnly;
import static com.combotto.controlplane.support.SecurityTestSupport.authenticatedWithoutTenant;
import static com.combotto.controlplane.support.SecurityTestSupport.writeOnly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.combotto.controlplane.repositories.CertificateRepository;

import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {

  @Container
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CertificateRepository certificateRepository;

  @BeforeEach
  void setUp() {
    certificateRepository.deleteAll();
  }

  @Test
  void readScope_canListCertificates() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .with(readOnly()))
        .andExpect(status().isOk());
  }

  @Test
  void writeOnlyScope_cannotListCertificates() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .with(writeOnly()))
        .andExpect(status().isForbidden());
  }

  @Test
  void readOnlyScope_cannotCreateCertificate() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(readOnly())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateCertificateJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void writeScope_canCreateCertificate() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(writeOnly())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateCertificateJson()))
        .andExpect(status().isCreated());
  }

  @Test
  void authenticatedRequest_withoutTenantClaim_isUnauthorized() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .with(authenticatedWithoutTenant()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unauthenticated_cannotReadActuatorMetrics() throws Exception {
    mockMvc.perform(get("/actuator/metrics"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void readScope_canReadActuatorMetrics() throws Exception {
    mockMvc.perform(get("/actuator/metrics")
        .with(readOnly()))
        .andExpect(status().isOk());
  }

  @Test
  void writeOnlyScope_cannotReadActuatorMetrics() throws Exception {
    mockMvc.perform(get("/actuator/metrics")
        .with(writeOnly()))
        .andExpect(status().isForbidden());
  }

  @Test
  void unauthenticated_cannotReadActuatorPrometheus() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void readScope_canReadActuatorPrometheus() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")
        .with(readOnly()))
        .andExpect(status().isOk());
  }

  @Test
  void writeOnlyScope_cannotReadActuatorPrometheus() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")
        .with(writeOnly()))
        .andExpect(status().isForbidden());
  }

  private String validCreateCertificateJson() throws Exception {
    return """
        {
          "tenantId": "demo-tenant",
          "name": "Authorization Test Certificate",
          "commonName": "authz.example.com",
          "issuer": "Combotto CA",
          "serialNumber": "authz-123",
          "sha256Fingerprint": "AA:BB:CC:DD",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "local-dev",
          "notes": "authorization test"
        }
        """;
  }
}
