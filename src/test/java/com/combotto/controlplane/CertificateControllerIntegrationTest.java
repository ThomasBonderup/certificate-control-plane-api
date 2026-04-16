package com.combotto.controlplane;

import java.time.OffsetDateTime;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.repositories.CertificateRepository;
import com.combotto.controlplane.support.CertificateFixtures;

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
    String responseBody = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
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
    UUID id = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);

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
  void getById_returns404_whenCertificateDoesNotExist() throws Exception {
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
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "demo-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
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
  void list_returns200_andOnlyTenantIdCerts() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .param("tenantId", "demo-tenant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$[0].name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$[0].renewalStatus").value("NOT_STATUS"));
  }

  @Test
  void list_returns200_andOnlyCertificateStatusActive() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$[0].name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$[0].renewalStatus").value("NOT_STATUS"));
  }

  @Test
  void list_returns200_andOnlyRenewalStatusInProgress() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].tenantId").value("test-tenant"))
        .andExpect(jsonPath("$[0].name").value("Gateway Client Certificate"))
        .andExpect(jsonPath("$[0].status").value("EXPIRING_SOON"))
        .andExpect(jsonPath("$[0].renewalStatus").value("IN_PROGRESS"));
  }

  @Test
  void list_returns200_whenMultipleFiltersAreCombined() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "demo-tenant",
            "Matching Certificate",
            "match.example.com",
            "Combotto CA",
            "111111111",
            "AA:AA:AA:AA",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.ACTIVE,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "matches all filters"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "demo-tenant",
            "Wrong Renewal",
            "renewal.example.com",
            "Combotto CA",
            "222222222",
            "BB:BB:BB:BB",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.ACTIVE,
            RenewalStatus.PLANNED,
            "platform-team",
            "matches tenant and status only"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "other-tenant",
            "Wrong Tenant",
            "tenant.example.com",
            "Combotto CA",
            "333333333",
            "CC:CC:CC:CC",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.ACTIVE,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "matches status and renewal only"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .param("tenantId", "demo-tenant")
        .param("status", "ACTIVE")
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$[0].name").value("Matching Certificate"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$[0].renewalStatus").value("IN_PROGRESS"));
  }

  @Test
  void list_returnsAllCertificates_whenTenantIdFilterIsEmptyString() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(CertificateFixtures.validCreateRequest(
            "other-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-08-01T00:00:00Z"),
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .param("tenantId", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].tenantId",
            org.hamcrest.Matchers.containsInAnyOrder(
                "demo-tenant",
                "other-tenant")));
  }

  @Test
  void list_returns400_whenStatusFilterHasInvalidEnumValue() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .param("status", "INVALID_STATUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status': INVALID_STATUS"))
        .andExpect(jsonPath("$.path").value("/api/certificates"));
  }

  @Test
  void listExpiringSoon_returns200_andOnlyCertificatesWithinDefault30DayWindowOrderedByNotAfter()
      throws Exception {
    OffsetDateTime now = OffsetDateTime.now();

    CreateCertificateRequest expiringSoonFirst = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "First Expiring Certificate",
        "first.example.com",
        "Let's Encrypt",
        "111111111",
        "AA:AA:AA:AA",
        now.minusDays(30),
        now.plusDays(5),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "expires first");

    CreateCertificateRequest expiringSoonSecond = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Second Expiring Certificate",
        "second.example.com",
        "Combotto CA",
        "222222222",
        "BB:BB:BB:BB",
        now.minusDays(15),
        now.plusDays(20),
        CertificateStatus.EXPIRING_SOON,
        RenewalStatus.PLANNED,
        "platform-team",
        "expires second");

    CreateCertificateRequest outsideWindow = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Later Certificate",
        "later.example.com",
        "Combotto CA",
        "333333333",
        "CC:CC:CC:CC",
        now.minusDays(10),
        now.plusDays(45),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "platform-team",
        "outside default window");

    CreateCertificateRequest alreadyExpired = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expired Certificate",
        "expired.example.com",
        "Combotto CA",
        "444444444",
        "DD:DD:DD:DD",
        now.minusDays(60),
        now.minusDays(1),
        CertificateStatus.EXPIRED,
        RenewalStatus.NOT_STATUS,
        "platform-team",
        "already expired");

    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonFirst);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonSecond);
    CertificateFixtures.create(mockMvc, objectMapper, outsideWindow);
    CertificateFixtures.create(mockMvc, objectMapper, alreadyExpired);

    mockMvc.perform(get("/api/certificates/expiring-soon"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("First Expiring Certificate"))
        .andExpect(jsonPath("$[1].name").value("Second Expiring Certificate"));
  }

  @Test
  void listExpiringSoon_returns200_whenFiltersAreCombined() throws Exception {
    OffsetDateTime now = OffsetDateTime.now();

    CreateCertificateRequest matchingFirst = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "First Matching Certificate",
        "first-match.example.com",
        "Let's Encrypt",
        "555555555",
        "EE:EE:EE:EE",
        now.minusDays(30),
        now.plusDays(5),
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "platform-team",
        "matches all filters and expires first");

    CreateCertificateRequest matchingSecond = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Second Matching Certificate",
        "second-match.example.com",
        "Combotto CA",
        "666666666",
        "FF:FF:FF:FF",
        now.minusDays(20),
        now.plusDays(8),
        CertificateStatus.EXPIRING_SOON,
        RenewalStatus.IN_PROGRESS,
        "platform-team",
        "matches all filters and expires second");

    CreateCertificateRequest wrongOwner = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Wrong Owner Certificate",
        "wrong-owner.example.com",
        "Combotto CA",
        "777777777",
        "11:11:11:11",
        now.minusDays(20),
        now.plusDays(7),
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "other-owner",
        "wrong owner");

    CreateCertificateRequest wrongTenant = CertificateFixtures.validCreateRequest(
        "other-tenant",
        "Wrong Tenant Certificate",
        "wrong-tenant.example.com",
        "Combotto CA",
        "888888888",
        "22:22:22:22",
        now.minusDays(20),
        now.plusDays(6),
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "platform-team",
        "wrong tenant");

    CreateCertificateRequest wrongRenewalStatus = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Wrong Renewal Status Certificate",
        "wrong-renewal.example.com",
        "Combotto CA",
        "999999999",
        "33:33:33:33",
        now.minusDays(20),
        now.plusDays(4),
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "platform-team",
        "wrong renewal status");

    CreateCertificateRequest outsideDaysWindow = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Outside Days Window Certificate",
        "outside-window.example.com",
        "Combotto CA",
        "101010101",
        "44:44:44:44",
        now.minusDays(20),
        now.plusDays(20),
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "platform-team",
        "outside custom days window");

    CertificateFixtures.create(mockMvc, objectMapper, matchingFirst);
    CertificateFixtures.create(mockMvc, objectMapper, matchingSecond);
    CertificateFixtures.create(mockMvc, objectMapper, wrongOwner);
    CertificateFixtures.create(mockMvc, objectMapper, wrongTenant);
    CertificateFixtures.create(mockMvc, objectMapper, wrongRenewalStatus);
    CertificateFixtures.create(mockMvc, objectMapper, outsideDaysWindow);

    mockMvc.perform(get("/api/certificates/expiring-soon")
        .param("days", "10")
        .param("tenantId", "demo-tenant")
        .param("owner", "platform-team")
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("First Matching Certificate"))
        .andExpect(jsonPath("$[1].name").value("Second Matching Certificate"))
        .andExpect(jsonPath("$[*].tenantId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("demo-tenant"))))
        .andExpect(jsonPath("$[*].owner",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("platform-team"))))
        .andExpect(jsonPath("$[*].renewalStatus",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("IN_PROGRESS"))));
  }

  @Test
  void update_returns200_andUpdatedCertificate() throws Exception {
    UUID id = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);

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
    UUID id = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);

    mockMvc.perform(delete("/api/certificates/{id}", id))
        .andExpect(status().isNoContent());

    assertThat(certificateRepository.existsById(id)).isFalse();
  }

  @Test
  void summary_returns200_andCertificateSummary() throws Exception {
    OffsetDateTime now = OffsetDateTime.now();

    CreateCertificateRequest activeCertificate = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Active Broker Certificate",
        "active.example.com",
        "Let's Encrypt",
        "111111111",
        "AA:AA:AA:AA",
        now.minusDays(30),
        now.plusDays(60),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "counts toward total and active");

    CreateCertificateRequest expiringSoonCertificate = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon Gateway Certificate",
        "expiring.example.com",
        "Combotto CA",
        "222222222",
        "BB:BB:BB:BB",
        now.minusDays(10),
        now.plusDays(10),
        CertificateStatus.EXPIRING_SOON,
        RenewalStatus.PLANNED,
        "platform-team",
        "counts toward total and expiringSoon");

    CreateCertificateRequest renewalInProgressCertificate = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Renewal In Progress Certificate",
        "renewal.example.com",
        "Combotto CA",
        "333333333",
        "CC:CC:CC:CC",
        now.minusDays(5),
        now.plusDays(60),
        CertificateStatus.EXPIRING_SOON,
        RenewalStatus.IN_PROGRESS,
        "platform-team",
        "counts toward total and renewalProgress");

    CertificateFixtures.create(mockMvc, objectMapper, activeCertificate);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonCertificate);
    CertificateFixtures.create(mockMvc, objectMapper, renewalInProgressCertificate);

    mockMvc.perform(get("/api/certificates/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.active").value(1))
        .andExpect(jsonPath("$.expiredSoon").value(1))
        .andExpect(jsonPath("$.expired").value(0))
        .andExpect(jsonPath("$.renewalProgress").value(1));
  }
}
