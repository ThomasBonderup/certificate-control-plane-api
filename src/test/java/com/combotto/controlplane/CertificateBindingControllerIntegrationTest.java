package com.combotto.controlplane;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.combotto.controlplane.model.BindingType;
import com.combotto.controlplane.repositories.AssetRepository;
import com.combotto.controlplane.repositories.CertificateBindingRepository;
import com.combotto.controlplane.repositories.CertificateRepository;
import com.combotto.controlplane.support.AssetFixtures;
import com.combotto.controlplane.support.CertificateBindingFixtures;
import com.combotto.controlplane.support.CertificateFixtures;
import static com.combotto.controlplane.support.SecurityTestSupport.authenticated;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class CertificateBindingControllerIntegrationTest {

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
  private AssetRepository assetRepository;

  @Autowired
  private CertificateRepository certificateRepository;

  @Autowired
  private CertificateBindingRepository certificateBindingRepository;

  @BeforeEach
  void setUp() {
    certificateBindingRepository.deleteAll();
    assetRepository.deleteAll();
    certificateRepository.deleteAll();
  }

  @Test
  void create_returns201_location_body_and_persistsCertificateBinding() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    Long assetId = AssetFixtures.createAndReturnId(assetRepository);

    String responseBody = mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, assetId)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
            ".*/api/certificates/.*/bindings")))
        .andExpect(jsonPath("$.certificateId").value(certificateId.toString()))
        .andExpect(jsonPath("$.certificateName").value("Broker TLS Certificate"))
        .andExpect(jsonPath("$.assetId").value(assetId.intValue()))
        .andExpect(jsonPath("$.assetName").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.bindingType").value("MQTT_ENDPOINT"))
        .andExpect(jsonPath("$.endpoint").value("mqtt.example.com"))
        .andExpect(jsonPath("$.port").value(8883))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    UUID bindingId = UUID.fromString(body.get("id").asString());

    var saved = certificateBindingRepository.findById(bindingId);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getCertificate().getId()).isEqualTo(certificateId);
    assertThat(saved.orElseThrow().getAsset().getId()).isEqualTo(assetId);
    assertThat(saved.orElseThrow().getBindingType()).isEqualTo(BindingType.MQTT_ENDPOINT);
    assertThat(saved.orElseThrow().getEndpoint()).isEqualTo("mqtt.example.com");
    assertThat(saved.orElseThrow().getPort()).isEqualTo(8883);
  }

  @Test
  void create_returns404_whenCertificateDoesNotExist() throws Exception {
    UUID missingCertificateId = UUID.randomUUID();
    Long assetId = AssetFixtures.createAndReturnId(assetRepository);

    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", missingCertificateId)
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, assetId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Certificate not found: " + missingCertificateId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + missingCertificateId + "/bindings"));
  }

  @Test
  void create_returns404_whenAssetDoesNotExist() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    Long missingAssetId = 9999L;

    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, missingAssetId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + certificateId + "/bindings"));
  }

  @Test
  void create_returns404_whenAssetIsSoftDeleted() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    Long deletedAssetId = AssetFixtures.createDeletedAndReturnId(assetRepository, "Deleted Asset");

    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, deletedAssetId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Asset not found: " + deletedAssetId));
  }

  @Test
  void listByCertificateId_returns200_andOnlyBindingsForCertificate() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Broker TLS Certificate");
    UUID otherCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Gateway Client Certificate");

    Long gatewayAssetId = AssetFixtures.createAndReturnId(assetRepository);
    Long brokerAssetId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Broker Asset");
    Long unrelatedAssetId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Unrelated Asset");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        gatewayAssetId,
        BindingType.MQTT_ENDPOINT,
        "mqtt.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        brokerAssetId,
        BindingType.HTTPS_ENDPOINT,
        "https://broker.example.com",
        443);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        otherCertificateId,
        unrelatedAssetId,
        BindingType.DEVICE_CERT,
        null,
        null);

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content[*].certificateId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(certificateId.toString()))))
        .andExpect(jsonPath("$.content[*].certificateName",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Broker TLS Certificate"))))
        .andExpect(jsonPath("$.content[*].assetId",
            org.hamcrest.Matchers.containsInAnyOrder(
                gatewayAssetId.intValue(),
                brokerAssetId.intValue())))
        .andExpect(jsonPath("$.content[*].assetName",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Primary Gateway Asset",
                "Broker Asset")))
        .andExpect(jsonPath("$.content[*].bindingType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "MQTT_ENDPOINT",
                "HTTPS_ENDPOINT")));
  }

  @Test
  void listByCertificateId_respectsRequestedPageSize() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Broker TLS Certificate");
    Long assetCId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset C");
    Long assetAId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset A");
    Long assetBId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset B");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetCId,
        BindingType.MQTT_ENDPOINT,
        "charlie.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetAId,
        BindingType.MQTT_ENDPOINT,
        "alpha.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetBId,
        BindingType.MQTT_ENDPOINT,
        "bravo.example.com",
        8883);

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "endpoint,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.numberOfElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].endpoint").value("alpha.example.com"))
        .andExpect(jsonPath("$.content[1].endpoint").value("bravo.example.com"));
  }

  @Test
  void listByCertificateId_returnsRemainingBindingsOnSecondPage() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Broker TLS Certificate");
    Long assetAId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset A");
    Long assetBId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset B");
    Long assetCId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset C");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetAId,
        BindingType.MQTT_ENDPOINT,
        "alpha.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetBId,
        BindingType.MQTT_ENDPOINT,
        "bravo.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetCId,
        BindingType.MQTT_ENDPOINT,
        "charlie.example.com",
        8883);

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "endpoint,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].endpoint").value("charlie.example.com"));
  }

  @Test
  void listByCertificateId_sortsBindingsByEndpointAscendingWhenRequested() throws Exception {
    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Broker TLS Certificate");
    Long assetZuluId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset Zulu");
    Long assetAlphaId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset Alpha");
    Long assetMiddleId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset Middle");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetZuluId,
        BindingType.MQTT_ENDPOINT,
        "zulu.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetAlphaId,
        BindingType.MQTT_ENDPOINT,
        "alpha.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetMiddleId,
        BindingType.MQTT_ENDPOINT,
        "middle.example.com",
        8883);

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", certificateId)
        .with(authenticated())
        .param("sort", "endpoint,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].endpoint").value("alpha.example.com"))
        .andExpect(jsonPath("$.content[1].endpoint").value("middle.example.com"))
        .andExpect(jsonPath("$.content[2].endpoint").value("zulu.example.com"));
  }

  @Test
  void listByCertificateId_returns404_whenCertificateDoesNotExist() throws Exception {
    UUID missingCertificateId = UUID.randomUUID();

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", missingCertificateId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Certificate not found: " + missingCertificateId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + missingCertificateId + "/bindings"));
  }
}
