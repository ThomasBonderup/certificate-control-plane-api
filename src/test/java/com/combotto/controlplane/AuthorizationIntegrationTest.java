package com.combotto.controlplane;

import static com.combotto.controlplane.support.SecurityTestSupport.readOnly;
import static com.combotto.controlplane.support.SecurityTestSupport.adminAuthenticated;
import static com.combotto.controlplane.support.SecurityTestSupport.authenticatedWithoutTenant;
import static com.combotto.controlplane.support.SecurityTestSupport.writeOnly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

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
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
      .withInitScript("combotto-assets-test-schema.sql");

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
  void writeOnlyScope_cannotDeleteCertificate_withoutAdminRole() throws Exception {
    String body = mockMvc.perform(post("/api/certificates")
        .with(writeOnly())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateCertificateJson()))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String certificateId = objectMapper.readTree(body).get("id").asText();

    mockMvc.perform(delete("/api/certificates/{id}", certificateId)
        .with(writeOnly()))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminRole_withWriteScope_canDeleteCertificate() throws Exception {
    String body = mockMvc.perform(post("/api/certificates")
        .with(adminAuthenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateCertificateJson()))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String certificateId = objectMapper.readTree(body).get("id").asText();

    mockMvc.perform(delete("/api/certificates/{id}", certificateId)
        .with(adminAuthenticated()))
        .andExpect(status().isNoContent());
  }

  @Test
  void authenticatedRequest_withoutTenantClaim_isUnauthorized() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .with(authenticatedWithoutTenant()))
        .andExpect(status().isUnauthorized())
        .andExpect(status().reason(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
        .andExpect(jsonPath("$.path").value("/api/certificates"));
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
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("combotto_certificate_metrics_refresh_success")));
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
