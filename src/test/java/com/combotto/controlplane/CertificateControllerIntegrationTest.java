package com.combotto.controlplane;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.combotto.controlplane.repositories.CertificateRepository;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class CertificateControllerIntegrationTest {

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
  void create_returns201_location_body_and_persistsCertificate() throws Exception {
    String requestBody = """
        {
          "tenantId": "demo-tenant",
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    String responseBody = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/certificates/.*")))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.renewalStatus").value("NOT_STATUS"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    UUID id = UUID.fromString(body.get("id").asString());

    var saved = certificateRepository.findById(id);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getTenantId()).isEqualTo("demo-tenant");
    assertThat(saved.orElseThrow().getName()).isEqualTo("Broker TLS Certificate");
    assertThat(saved.orElseThrow().getIssuer()).isEqualTo("Let's Encrypt");
  }

  @Test
  void create_returns400_whenTenantIdIsMissing() throws Exception {
    String requestBody = """
        {
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.path").value("/api/certificates"));
  }

  @Test
  void getById_returns200_andBody() throws Exception {
    String requestBody = """
        {
          "tenantId": "demo-tenant",
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    String responseBody = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/certificates/.*")))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    UUID id = UUID.fromString(body.get("id").asString());

    mockMvc.perform(get("/api/certificates/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.commonName").value("mqtt.example.com"))
        .andExpect(jsonPath("$.issuer").value("Let's Encrypt"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.renewalStatus").value("NOT_STATUS"))
        .andExpect(jsonPath("$.owner").value("thomas"))
        .andExpect(jsonPath("$.notes").value("first certificate"));
  }

  @Test
  void getById_returns404_whenCertificateDoesNotExists() throws Exception {
    UUID missingId = UUID.randomUUID();

    mockMvc.perform(get("/api/certificates/{id}", missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Certificate not found: " + missingId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + missingId));
  }

  @Test
  void list_returns200_andAllCertificates() throws Exception {
    String firstRequest = """
        {
          "tenantId": "demo-tenant",
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    String secondRequest = """
        {
          "tenantId": "demo-tenant",
          "name": "Gateway Client Certificate",
          "commonName": "gateway.example.com",
          "issuer": "Combotto CA",
          "serialNumber": "987654321",
          "sha256Fingerprint": "12:34:56:78:90",
          "notBefore": "2026-05-01T00:00:00Z",
          "notAfter": "2026-08-01T00:00:00Z",
          "status": "EXPIRING_SOON",
          "renewalStatus": "PLANNED",
          "owner": "platform-team",
          "notes": "second certificate"
        }
        """;

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(firstRequest))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(secondRequest))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Broker TLS Certificate",
                "Gateway Client Certificate")))
        .andExpect(jsonPath("$[*].status",
            org.hamcrest.Matchers.containsInAnyOrder(
                "ACTIVE",
                "EXPIRING_SOON")));
  }

  @Test
  void update_returns200_andUpdatedCertificate() throws Exception {
    String createRequest = """
        {
          "tenantId": "demo-tenant",
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    String createResponse = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createRequest))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    UUID id = UUID.fromString(created.get("id").asString());

    String updateRequest = """
        {
          "name": "Updated Broker TLS Certificate",
          "status": "EXPIRING_SOON",
          "renewalStatus": "PLANNED",
          "owner": "platform-team",
          "notes": "updated certificate"
        }
        """;

    mockMvc.perform(patch("/api/certificates/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("Updated Broker TLS Certificate"))
        .andExpect(jsonPath("$.status").value("EXPIRING_SOON"))
        .andExpect(jsonPath("$.renewalStatus").value("PLANNED"))
        .andExpect(jsonPath("$.owner").value("platform-team"))
        .andExpect(jsonPath("$.notes").value("updated certificate"));

    var saved = certificateRepository.findById(id);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getName()).isEqualTo("Updated Broker TLS Certificate");
    assertThat(saved.orElseThrow().getOwner()).isEqualTo("platform-team");
  }

  @Test
  void delete_returns204_andRemovesCertificate() throws Exception {
    String createRequest = """
        {
          "tenantId": "demo-tenant",
          "name": "Broker TLS Certificate",
          "commonName": "mqtt.example.com",
          "issuer": "Let's Encrypt",
          "serialNumber": "123456789",
          "sha256Fingerprint": "AB:CD:EF:12:34",
          "notBefore": "2026-04-01T00:00:00Z",
          "notAfter": "2026-07-01T00:00:00Z",
          "status": "ACTIVE",
          "renewalStatus": "NOT_STATUS",
          "owner": "thomas",
          "notes": "first certificate"
        }
        """;

    String createResponse = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createRequest))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    UUID id = UUID.fromString(created.get("id").asString());

    mockMvc.perform(delete("/api/certificates/{id}", id))
        .andExpect(status().isNoContent());

    assertThat(certificateRepository.existsById(id)).isFalse();
  }
}
