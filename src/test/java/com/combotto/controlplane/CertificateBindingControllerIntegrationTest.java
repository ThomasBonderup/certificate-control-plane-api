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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class CertificateBindingControllerIntegrationTest {

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
    UUID certificateId = createCertificateAndReturnId();
    UUID assetId = createAssetAndReturnId();

    String responseBody = mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, assetId)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
            ".*/api/certificates/.*/bindings")))
        .andExpect(jsonPath("$.certificateId").value(certificateId.toString()))
        .andExpect(jsonPath("$.assetId").value(assetId.toString()))
        .andExpect(jsonPath("$.assetName").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.bindingType").value("MQTT_ENDPOINT"))
        .andExpect(jsonPath("$.endpoint").value("mqtt.example.com"))
        .andExpect(jsonPath("$.port").value(8883))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    UUID bindingId = UUID.fromString(body.get("id").asText());

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
    UUID assetId = createAssetAndReturnId();

    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", missingCertificateId)
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
    UUID certificateId = createCertificateAndReturnId();
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(objectMapper, missingAssetId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + certificateId + "/bindings"));
  }

  @Test
  void listByCertificateId_returns200_andOnlyBindingsForCertificate() throws Exception {
    UUID certificateId = createCertificateAndReturnId();
    UUID otherCertificateId = createCertificateAndReturnId();

    UUID gatewayAssetId = createAssetAndReturnId();
    UUID brokerAssetId = createAssetAndReturnId("demo-tenant", "Broker Asset");
    UUID unrelatedAssetId = createAssetAndReturnId("demo-tenant", "Unrelated Asset");

    createBinding(certificateId, gatewayAssetId, BindingType.MQTT_ENDPOINT, "mqtt.example.com", 8883);
    createBinding(certificateId, brokerAssetId, BindingType.HTTPS_ENDPOINT, "https://broker.example.com", 443);
    createBinding(otherCertificateId, unrelatedAssetId, BindingType.DEVICE_CERT, null, null);

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", certificateId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].certificateId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(certificateId.toString()))))
        .andExpect(jsonPath("$[*].assetId",
            org.hamcrest.Matchers.containsInAnyOrder(
                gatewayAssetId.toString(),
                brokerAssetId.toString())))
        .andExpect(jsonPath("$[*].assetName",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Primary Gateway Asset",
                "Broker Asset")))
        .andExpect(jsonPath("$[*].bindingType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "MQTT_ENDPOINT",
                "HTTPS_ENDPOINT")));
  }

  @Test
  void listByCertificateId_returns404_whenCertificateDoesNotExist() throws Exception {
    UUID missingCertificateId = UUID.randomUUID();

    mockMvc.perform(get("/api/certificates/{certificateId}/bindings", missingCertificateId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Certificate not found: " + missingCertificateId))
        .andExpect(jsonPath("$.path").value("/api/certificates/" + missingCertificateId + "/bindings"));
  }

  private UUID createCertificateAndReturnId() throws Exception {
    String responseBody = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    return UUID.fromString(body.get("id").asString());
  }

  private UUID createAssetAndReturnId() throws Exception {
    String responseBody = mockMvc.perform(post("/api/assets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(AssetFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    return UUID.fromString(body.get("id").asString());
  }

  private UUID createAssetAndReturnId(String tenantId, String name) throws Exception {
    String responseBody = mockMvc.perform(post("/api/assets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(AssetFixtures.validCreateRequestJson(
            objectMapper,
            AssetFixtures.validCreateRequest(
                tenantId,
                name,
                com.combotto.controlplane.model.AssetType.GATEWAY,
                "production",
                name.toLowerCase().replace(" ", "-") + ".example.com",
                "eu-west-1"))))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    return UUID.fromString(body.get("id").asString());
  }

  private void createBinding(
      UUID certificateId,
      UUID assetId,
      BindingType bindingType,
      String endpoint,
      Integer port) throws Exception {
    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(CertificateBindingFixtures.validCreateRequestJson(
            objectMapper,
            CertificateBindingFixtures.validCreateRequest(assetId, bindingType, endpoint, port))))
        .andExpect(status().isCreated());
  }
}
