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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AssetControllerIntegrationTest {

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
  void create_returns201_location_body_and_persistsAsset() throws Exception {
    String responseBody = mockMvc.perform(post("/api/assets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(AssetFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/assets/.*")))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.assetType").value("GATEWAY"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    UUID assetId = UUID.fromString(body.get("id").asString());

    var saved = assetRepository.findById(assetId);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getTenantId()).isEqualTo("demo-tenant");
    assertThat(saved.orElseThrow().getName()).isEqualTo("Primary Gateway Asset");
    assertThat(saved.orElseThrow().getHostname()).isEqualTo("gateway-01.example.com");
  }

  @Test
  void list_returns200_andAllAssets() throws Exception {
    createAssetAndReturnId();
    createAssetAndReturnId("demo-tenant", "Broker Asset");

    mockMvc.perform(get("/api/assets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Primary Gateway Asset",
                "Broker Asset")))
        .andExpect(jsonPath("$[*].assetType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "GATEWAY",
                "GATEWAY")));
  }

  @Test
  void getById_returns200_andBody() throws Exception {
    UUID assetId = createAssetAndReturnId();

    mockMvc.perform(get("/api/assets/{id}", assetId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId.toString()))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.assetType").value("GATEWAY"))
        .andExpect(jsonPath("$.environment").value("production"))
        .andExpect(jsonPath("$.hostname").value("gateway-01.example.com"))
        .andExpect(jsonPath("$.location").value("eu-west-1"));
  }

  @Test
  void getById_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(get("/api/assets/{id}", missingAssetId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void update_returns200_andUpdatesAsset() throws Exception {
    UUID assetId = createAssetAndReturnId();

    mockMvc.perform(patch("/api/assets/{id}", assetId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Updated Gateway Asset",
              "assetType": "GATEWAY",
              "environment": "staging",
              "hostname": "gateway-02.example.com",
              "location": "eu-central-1"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId.toString()))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Updated Gateway Asset"))
        .andExpect(jsonPath("$.assetType").value("GATEWAY"))
        .andExpect(jsonPath("$.environment").value("staging"))
        .andExpect(jsonPath("$.hostname").value("gateway-02.example.com"))
        .andExpect(jsonPath("$.location").value("eu-central-1"));

    var saved = assetRepository.findById(assetId);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getName()).isEqualTo("Updated Gateway Asset");
    assertThat(saved.orElseThrow().getEnvironment()).isEqualTo("staging");
    assertThat(saved.orElseThrow().getHostname()).isEqualTo("gateway-02.example.com");
    assertThat(saved.orElseThrow().getLocation()).isEqualTo("eu-central-1");
  }

  @Test
  void update_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(patch("/api/assets/{id}", missingAssetId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Updated Gateway Asset"
            }
            """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void delete_returns204_andRemovesAsset() throws Exception {
    UUID assetId = createAssetAndReturnId();

    mockMvc.perform(delete("/api/assets/{id}", assetId))
        .andExpect(status().isNoContent());

    assertThat(assetRepository.existsById(assetId)).isFalse();
  }

  @Test
  void delete_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(delete("/api/assets/{id}", missingAssetId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void listBindingsByAssetId_returns200_andOnlyBindingsForAsset() throws Exception {
    UUID assetId = createAssetAndReturnId();
    UUID otherAssetId = createAssetAndReturnId("demo-tenant", "Broker Asset");

    UUID certificateId = createCertificateAndReturnId();
    UUID otherCertificateId = createCertificateAndReturnId("Gateway Client Certificate");
    UUID unrelatedCertificateId = createCertificateAndReturnId("Unrelated Certificate");

    createBinding(certificateId, assetId, BindingType.MQTT_ENDPOINT, "mqtt.example.com", 8883);
    createBinding(otherCertificateId, assetId, BindingType.HTTPS_ENDPOINT, "https://broker.example.com", 443);
    createBinding(unrelatedCertificateId, otherAssetId, BindingType.DEVICE_CERT, null, null);

    mockMvc.perform(get("/api/assets/{assetId}/bindings", assetId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].assetId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(assetId.toString()))))
        .andExpect(jsonPath("$[*].certificateId",
            org.hamcrest.Matchers.containsInAnyOrder(
                certificateId.toString(),
                otherCertificateId.toString())))
        .andExpect(jsonPath("$[*].certificateName",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Broker TLS Certificate",
                "Gateway Client Certificate")))
        .andExpect(jsonPath("$[*].bindingType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "MQTT_ENDPOINT",
                "HTTPS_ENDPOINT")));
  }

  @Test
  void listBindingsByAssetId_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(get("/api/assets/{assetId}/bindings", missingAssetId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId + "/bindings"));
  }

  @Test
  void listCertificatesByAssetId_returns200_andOnlyCertificatesForAsset() throws Exception {
    UUID assetId = createAssetAndReturnId();
    UUID otherAssetId = createAssetAndReturnId("demo-tenant", "Broker Asset");

    UUID certificateId = createCertificateAndReturnId();
    UUID otherCertificateId = createCertificateAndReturnId("Gateway Client Certificate");
    UUID unrelatedCertificateId = createCertificateAndReturnId("Unrelated Certificate");

    createBinding(certificateId, assetId, BindingType.MQTT_ENDPOINT, "mqtt.example.com", 8883);
    createBinding(otherCertificateId, assetId, BindingType.HTTPS_ENDPOINT, "https://broker.example.com", 443);
    createBinding(unrelatedCertificateId, otherAssetId, BindingType.DEVICE_CERT, null, null);

    mockMvc.perform(get("/api/assets/{assetId}/certificates", assetId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].id",
            org.hamcrest.Matchers.containsInAnyOrder(
                certificateId.toString(),
                otherCertificateId.toString())))
        .andExpect(jsonPath("$[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Broker TLS Certificate",
                "Gateway Client Certificate")))
        .andExpect(jsonPath("$[*].tenantId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("demo-tenant"))));
  }

  @Test
  void listCertificatesByAssetId_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(get("/api/assets/{assetId}/certificates", missingAssetId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId + "/certificates"));
  }

  private UUID createAssetAndReturnId() throws Exception {
    return AssetFixtures.createAndReturnId(mockMvc, objectMapper);
  }

  private UUID createAssetAndReturnId(String tenantId, String name) throws Exception {
    return AssetFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        AssetFixtures.validCreateRequest(tenantId, name));
  }

  private UUID createCertificateAndReturnId() throws Exception {
    return CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
  }

  private UUID createCertificateAndReturnId(String name) throws Exception {
    return CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        CertificateFixtures.validCreateRequest(name));
  }

  private void createBinding(
      UUID certificateId,
      UUID assetId,
      BindingType bindingType,
      String endpoint,
      Integer port) throws Exception {
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        CertificateBindingFixtures.validCreateRequest(assetId, bindingType, endpoint, port));
  }
}
