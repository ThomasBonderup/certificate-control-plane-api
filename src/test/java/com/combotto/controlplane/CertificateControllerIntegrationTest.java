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
import static com.combotto.controlplane.support.SecurityTestSupport.authenticated;

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
    String currentUser = "certificate-creator";

    String responseBody = mockMvc.perform(post("/api/certificates")
        .with(authenticated(currentUser))
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/certificates/.*")))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.renewalStatus").value("NOT_STATUS"))
        .andExpect(jsonPath("$.blockedReason").isEmpty())
        .andExpect(jsonPath("$.renewalUpdatedAt").isEmpty())
        .andExpect(jsonPath("$.createdBy").value(currentUser))
        .andExpect(jsonPath("$.updatedBy").value(currentUser))
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
    assertThat(saved.orElseThrow().getBlockedReason()).isNull();
    assertThat(saved.orElseThrow().getRenewalUpdatedAt()).isNull();
    assertThat(saved.orElseThrow().getCreatedBy()).isEqualTo(currentUser);
    assertThat(saved.orElseThrow().getUpdatedBy()).isEqualTo(currentUser);
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
        .with(authenticated())
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

    mockMvc.perform(get("/api/certificates/{id}", id)
        .with(authenticated()))
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

    mockMvc.perform(get("/api/certificates/{id}", missingId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Certificate not found: " + missingId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + missingId));
  }

  @Test
  void list_returns200_andAllCertificates() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "demo-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            30,
            90,
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Broker TLS Certificate",
                "Gateway Client Certificate")))
        .andExpect(jsonPath("$.content[*].status",
            org.hamcrest.Matchers.containsInAnyOrder(
                "ACTIVE",
                "EXPIRING_SOON")));
  }

  @Test
  void list_returns200_andOnlyTenantIdCerts() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            30,
            90,
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("tenantId", "demo-tenant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.content[0].name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.content[0].renewalStatus").value("NOT_STATUS"));
  }

  @Test
  void list_returns200_andOnlyCertificateStatusActive() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            30,
            90,
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.content[0].name").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.content[0].renewalStatus").value("NOT_STATUS"));
  }

  @Test
  void list_returns200_andOnlyRenewalStatusInProgress() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "test-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            30,
            90,
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].tenantId").value("test-tenant"))
        .andExpect(jsonPath("$.content[0].name").value("Gateway Client Certificate"))
        .andExpect(jsonPath("$.content[0].status").value("EXPIRING_SOON"))
        .andExpect(jsonPath("$.content[0].renewalStatus").value("IN_PROGRESS"));
  }

  @Test
  void list_returns200_whenMultipleFiltersAreCombined() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "demo-tenant",
            "Matching Certificate",
            "match.example.com",
            "Combotto CA",
            "111111111",
            "AA:AA:AA:AA",
            30,
            90,
            CertificateStatus.ACTIVE,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "matches all filters"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "demo-tenant",
            "Wrong Renewal",
            "renewal.example.com",
            "Combotto CA",
            "222222222",
            "BB:BB:BB:BB",
            30,
            90,
            CertificateStatus.ACTIVE,
            RenewalStatus.PLANNED,
            "platform-team",
            "matches tenant and status only"))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "other-tenant",
            "Wrong Tenant",
            "tenant.example.com",
            "Combotto CA",
            "333333333",
            "CC:CC:CC:CC",
            30,
            90,
            CertificateStatus.ACTIVE,
            RenewalStatus.IN_PROGRESS,
            "platform-team",
            "matches status and renewal only"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("tenantId", "demo-tenant")
        .param("status", "ACTIVE")
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.content[0].name").value("Matching Certificate"))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.content[0].renewalStatus").value("IN_PROGRESS"));
  }

  @Test
  void list_returnsAllCertificates_whenTenantIdFilterIsEmptyString() throws Exception {
    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/certificates")
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(relativeCertificateRequest(
            "other-tenant",
            "Gateway Client Certificate",
            "gateway.example.com",
            "Combotto CA",
            "987654321",
            "12:34:56:78:90",
            30,
            90,
            CertificateStatus.EXPIRING_SOON,
            RenewalStatus.PLANNED,
            "platform-team",
            "second certificate"))))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("tenantId", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].tenantId",
            org.hamcrest.Matchers.containsInAnyOrder(
                "demo-tenant",
                "other-tenant")));
  }

  @Test
  void list_respectsRequestedPageSize() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate C",
        "cert-c.example.com",
        "Combotto CA",
        "serial-c",
        "fingerprint-c",
        30,
        33,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "third certificate"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate A",
        "cert-a.example.com",
        "Combotto CA",
        "serial-a",
        "fingerprint-a",
        30,
        31,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "first certificate"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate B",
        "cert-b.example.com",
        "Combotto CA",
        "serial-b",
        "fingerprint-b",
        30,
        32,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "second certificate"));

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "notAfter,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.numberOfElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Certificate A"))
        .andExpect(jsonPath("$.content[1].name").value("Certificate B"));
  }

  @Test
  void list_returnsRemainingItemsOnSecondPage() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate A",
        "cert-a.example.com",
        "Combotto CA",
        "serial-a",
        "fingerprint-a",
        30,
        31,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "first certificate"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate B",
        "cert-b.example.com",
        "Combotto CA",
        "serial-b",
        "fingerprint-b",
        30,
        32,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "second certificate"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Certificate C",
        "cert-c.example.com",
        "Combotto CA",
        "serial-c",
        "fingerprint-c",
        30,
        33,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "third certificate"));

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "notAfter,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Certificate C"));
  }

  @Test
  void list_sortsByNotAfterAscendingWhenRequested() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Late Certificate",
        "late.example.com",
        "Combotto CA",
        "serial-late",
        "fingerprint-late",
        30,
        45,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "expires last"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Early Certificate",
        "early.example.com",
        "Combotto CA",
        "serial-early",
        "fingerprint-early",
        30,
        31,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "expires first"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Middle Certificate",
        "middle.example.com",
        "Combotto CA",
        "serial-middle",
        "fingerprint-middle",
        30,
        40,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "expires second"));

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("sort", "notAfter,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].name").value("Early Certificate"))
        .andExpect(jsonPath("$.content[1].name").value("Middle Certificate"))
        .andExpect(jsonPath("$.content[2].name").value("Late Certificate"));
  }

  @Test
  void list_combinesStatusFilterWithPaginationMetadata() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Active Certificate A",
        "active-a.example.com",
        "Combotto CA",
        "serial-active-a",
        "fingerprint-active-a",
        30,
        31,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "active first"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Expiring Certificate",
        "expiring.example.com",
        "Combotto CA",
        "serial-expiring",
        "fingerprint-expiring",
        30,
        32,
        CertificateStatus.EXPIRING_SOON,
        RenewalStatus.PLANNED,
        "thomas",
        "not active"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Active Certificate B",
        "active-b.example.com",
        "Combotto CA",
        "serial-active-b",
        "fingerprint-active-b",
        30,
        33,
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "thomas",
        "active second"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Active Certificate C",
        "active-c.example.com",
        "Combotto CA",
        "serial-active-c",
        "fingerprint-active-c",
        30,
        34,
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "thomas",
        "active third"));

    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
        .param("status", "ACTIVE")
        .param("page", "0")
        .param("size", "2")
        .param("sort", "notAfter,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[*].status",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.numberOfElements").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Active Certificate A"))
        .andExpect(jsonPath("$.content[1].name").value("Active Certificate B"));
  }

  @Test
  void list_returns400_whenStatusFilterHasInvalidEnumValue() throws Exception {
    mockMvc.perform(get("/api/certificates")
        .with(authenticated())
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

    mockMvc.perform(get("/api/certificates/expiring-soon")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[0].name").value("First Expiring Certificate"))
        .andExpect(jsonPath("$.content[1].name").value("Second Expiring Certificate"));
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
        .with(authenticated())
        .param("days", "10")
        .param("tenantId", "demo-tenant")
        .param("owner", "platform-team")
        .param("renewalStatus", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].name").value("First Matching Certificate"))
        .andExpect(jsonPath("$.content[1].name").value("Second Matching Certificate"))
        .andExpect(jsonPath("$.content[*].tenantId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("demo-tenant"))))
        .andExpect(jsonPath("$.content[*].owner",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("platform-team"))))
        .andExpect(jsonPath("$.content[*].renewalStatus",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("IN_PROGRESS"))));
  }

  @Test
  void listExpiringSoon_supportsPaginationAndSorting() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Zulu Expiring",
        "zulu-expiring.example.com",
        "Combotto CA",
        "serial-zulu-expiring",
        "fingerprint-zulu-expiring",
        20,
        12,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "zulu expiring"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Alpha Expiring",
        "alpha-expiring.example.com",
        "Combotto CA",
        "serial-alpha-expiring",
        "fingerprint-alpha-expiring",
        20,
        10,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "alpha expiring"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Middle Expiring",
        "middle-expiring.example.com",
        "Combotto CA",
        "serial-middle-expiring",
        "fingerprint-middle-expiring",
        20,
        11,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "middle expiring"));

    mockMvc.perform(get("/api/certificates/expiring-soon")
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Alpha Expiring"))
        .andExpect(jsonPath("$.content[1].name").value("Middle Expiring"));

    mockMvc.perform(get("/api/certificates/expiring-soon")
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Zulu Expiring"));
  }

  @Test
  void listAttentionNeeded_returns200_andIncludesExpiredAndWindowedAttentionCases() throws Exception {
    OffsetDateTime now = OffsetDateTime.now();

    CreateCertificateRequest expiredCertificate = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expired Certificate",
        "expired.example.com",
        "Let's Encrypt",
        "121212121",
        "55:55:55:55",
        now.minusDays(60),
        now.minusDays(1),
        CertificateStatus.EXPIRED,
        RenewalStatus.PLANNED,
        "platform-team",
        "expired");

    CreateCertificateRequest expiringSoonNotStarted = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon Not Started Certificate",
        "not-started.example.com",
        "Combotto CA",
        "131313131",
        "66:66:66:66",
        now.minusDays(10),
        now.plusDays(8),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "expiring soon and not started");

    CreateCertificateRequest expiringSoonPlanned = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon Planned Certificate",
        "planned.example.com",
        "Combotto CA",
        "131313132",
        "66:66:66:67",
        now.minusDays(10),
        now.plusDays(10),
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "thomas",
        "expiring soon and planned");

    CreateCertificateRequest expiringSoonInProgress = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon In Progress Certificate",
        "in-progress.example.com",
        "Combotto CA",
        "131313133",
        "66:66:66:68",
        now.minusDays(10),
        now.plusDays(14),
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "thomas",
        "expiring soon and in progress");

    CreateCertificateRequest expiringSoonNoOwner = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon No Owner Certificate",
        "no-owner.example.com",
        "Combotto CA",
        "141414141",
        "77:77:77:77",
        now.minusDays(10),
        now.plusDays(12),
        CertificateStatus.ACTIVE,
        RenewalStatus.COMPLETED,
        "   ",
        "blank owner should count");

    CreateCertificateRequest blockedOutsideWindow = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Blocked Outside Window Certificate",
        "blocked.example.com",
        "Combotto CA",
        "151515151",
        "88:88:88:88",
        now.minusDays(10),
        now.plusDays(45),
        CertificateStatus.ACTIVE,
        RenewalStatus.BLOCKED,
        "platform-team",
        "blocked should count even outside window");

    CreateCertificateRequest laterNoOwner = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Later No Owner Certificate",
        "later-no-owner.example.com",
        "Combotto CA",
        "161616161",
        "99:99:99:99",
        now.minusDays(10),
        now.plusDays(45),
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "",
        "outside window should not count");

    CreateCertificateRequest laterPlanned = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Later Planned Certificate",
        "later-planned.example.com",
        "Combotto CA",
        "171717171",
        "AA:BB:CC:DD",
        now.minusDays(10),
        now.plusDays(50),
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "platform-team",
        "outside window should not count");

    CreateCertificateRequest expiringSoonCompletedOwned = CertificateFixtures.validCreateRequest(
        "demo-tenant",
        "Expiring Soon Completed Owned Certificate",
        "completed.example.com",
        "Combotto CA",
        "181818181",
        "EE:FF:GG:HH",
        now.minusDays(10),
        now.plusDays(16),
        CertificateStatus.ACTIVE,
        RenewalStatus.COMPLETED,
        "platform-team",
        "completed with owner should not count");

    CertificateFixtures.create(mockMvc, objectMapper, expiredCertificate);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonNotStarted);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonPlanned);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonInProgress);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonNoOwner);
    CertificateFixtures.create(mockMvc, objectMapper, blockedOutsideWindow);
    CertificateFixtures.create(mockMvc, objectMapper, laterNoOwner);
    CertificateFixtures.create(mockMvc, objectMapper, laterPlanned);
    CertificateFixtures.create(mockMvc, objectMapper, expiringSoonCompletedOwned);

    mockMvc.perform(get("/api/certificates/attention-needed")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(6))
        .andExpect(jsonPath("$.totalElements").value(6))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[0].name").value("Expired Certificate"))
        .andExpect(jsonPath("$.content[1].name").value("Expiring Soon Not Started Certificate"))
        .andExpect(jsonPath("$.content[2].name").value("Expiring Soon Planned Certificate"))
        .andExpect(jsonPath("$.content[3].name").value("Expiring Soon No Owner Certificate"))
        .andExpect(jsonPath("$.content[4].name").value("Expiring Soon In Progress Certificate"))
        .andExpect(jsonPath("$.content[5].name").value("Blocked Outside Window Certificate"));
  }

  @Test
  void listAttentionNeeded_supportsPaginationAndSorting() throws Exception {
    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Zulu Attention",
        "zulu-attention.example.com",
        "Combotto CA",
        "serial-zulu-attention",
        "fingerprint-zulu-attention",
        20,
        5,
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "zulu attention"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Alpha Attention",
        "alpha-attention.example.com",
        "Combotto CA",
        "serial-alpha-attention",
        "fingerprint-alpha-attention",
        20,
        7,
        CertificateStatus.ACTIVE,
        RenewalStatus.PLANNED,
        "thomas",
        "alpha attention"));

    CertificateFixtures.create(mockMvc, objectMapper, relativeCertificateRequest(
        "demo-tenant",
        "Middle Attention",
        "middle-attention.example.com",
        "Combotto CA",
        "serial-middle-attention",
        "fingerprint-middle-attention",
        20,
        6,
        CertificateStatus.ACTIVE,
        RenewalStatus.IN_PROGRESS,
        "thomas",
        "middle attention"));

    mockMvc.perform(get("/api/certificates/attention-needed")
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Alpha Attention"))
        .andExpect(jsonPath("$.content[1].name").value("Middle Attention"));

    mockMvc.perform(get("/api/certificates/attention-needed")
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Zulu Attention"));
  }

  @Test
  void update_returns200_andUpdatedCertificate() throws Exception {
    String createdBy = "certificate-creator";
    String updatedBy = "certificate-editor";

    UUID id = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        CertificateFixtures.validCreateRequest(),
        createdBy);

    String updateRequest = """
        {
          "name": "Updated Broker TLS Certificate",
          "status": "EXPIRING_SOON",
          "renewalStatus": "BLOCKED",
          "blockedReason": "Waiting for maintenance window",
          "owner": "platform-team",
          "notes": "updated certificate"
        }
        """;

    mockMvc.perform(patch("/api/certificates/{id}", id)
        .with(authenticated(updatedBy))
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("Updated Broker TLS Certificate"))
        .andExpect(jsonPath("$.status").value("EXPIRING_SOON"))
        .andExpect(jsonPath("$.renewalStatus").value("BLOCKED"))
        .andExpect(jsonPath("$.blockedReason").value("Waiting for maintenance window"))
        .andExpect(jsonPath("$.renewalUpdatedAt").isNotEmpty())
        .andExpect(jsonPath("$.owner").value("platform-team"))
        .andExpect(jsonPath("$.notes").value("updated certificate"))
        .andExpect(jsonPath("$.createdBy").value(createdBy))
        .andExpect(jsonPath("$.updatedBy").value(updatedBy));

    var saved = certificateRepository.findById(id);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getName()).isEqualTo("Updated Broker TLS Certificate");
    assertThat(saved.orElseThrow().getOwner()).isEqualTo("platform-team");
    assertThat(saved.orElseThrow().getBlockedReason()).isEqualTo("Waiting for maintenance window");
    assertThat(saved.orElseThrow().getRenewalUpdatedAt()).isNotNull();
    assertThat(saved.orElseThrow().getCreatedBy()).isEqualTo(createdBy);
    assertThat(saved.orElseThrow().getUpdatedBy()).isEqualTo(updatedBy);
  }

  @Test
  void update_returns400_whenBlockedStatusMissingBlockedReason() throws Exception {
    UUID id = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);

    String updateRequest = """
        {
          "renewalStatus": "BLOCKED"
        }
        """;

    mockMvc.perform(patch("/api/certificates/{id}", id)
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("blockedReason is required when renewalStatus is BLOCKED"))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + id));
  }

  @Test
  void delete_returns204_andRemovesCertificate() throws Exception {
    UUID id = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);

    mockMvc.perform(delete("/api/certificates/{id}", id)
        .with(authenticated()))
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

    mockMvc.perform(get("/api/certificates/summary")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.active").value(1))
        .andExpect(jsonPath("$.expiredSoon").value(1))
        .andExpect(jsonPath("$.expired").value(0))
        .andExpect(jsonPath("$.renewalProgress").value(1));
  }

  private CreateCertificateRequest relativeCertificateRequest(
      String tenantId,
      String name,
      String commonName,
      String issuer,
      String serialNumber,
      String sha256Fingerprint,
      long notBeforeDaysAgo,
      long notAfterDaysFromNow,
      CertificateStatus status,
      RenewalStatus renewalStatus,
      String owner,
      String notes) {
    OffsetDateTime now = OffsetDateTime.now();

    return CertificateFixtures.validCreateRequest(
        tenantId,
        name,
        commonName,
        issuer,
        serialNumber,
        sha256Fingerprint,
        now.minusDays(notBeforeDaysAgo),
        now.plusDays(notAfterDaysFromNow),
        status,
        renewalStatus,
        owner,
        notes);
  }
}
