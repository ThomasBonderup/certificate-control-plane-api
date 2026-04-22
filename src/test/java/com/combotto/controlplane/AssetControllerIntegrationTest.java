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
import static com.combotto.controlplane.support.SecurityTestSupport.adminAuthenticated;
import static com.combotto.controlplane.support.SecurityTestSupport.authenticated;

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
    String currentUser = "asset-creator";

    String responseBody = mockMvc.perform(post("/api/assets")
        .with(authenticated(currentUser))
        .contentType(MediaType.APPLICATION_JSON)
        .content(AssetFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/assets/.*")))
        .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
        .andExpect(jsonPath("$.name").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.assetType").value("GATEWAY"))
        .andExpect(jsonPath("$.createdBy").value(currentUser))
        .andExpect(jsonPath("$.updatedBy").value(currentUser))
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
    assertThat(saved.orElseThrow().getCreatedBy()).isEqualTo(currentUser);
    assertThat(saved.orElseThrow().getUpdatedBy()).isEqualTo(currentUser);
  }

  @Test
  void list_returns200_andAllAssets() throws Exception {
    AssetFixtures.createAndReturnId(mockMvc, objectMapper);
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Broker Asset");

    mockMvc.perform(get("/api/assets")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Primary Gateway Asset",
                "Broker Asset")))
        .andExpect(jsonPath("$.content[*].assetType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "GATEWAY",
                "GATEWAY")));
  }

  @Test
  void list_respectsRequestedPageSize() throws Exception {
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset C");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset A");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset B");

    mockMvc.perform(get("/api/assets")
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.numberOfElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Asset A"))
        .andExpect(jsonPath("$.content[1].name").value("Asset B"));
  }

  @Test
  void list_returnsRemainingAssetsOnSecondPage() throws Exception {
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset A");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset B");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Asset C");

    mockMvc.perform(get("/api/assets")
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Asset C"));
  }

  @Test
  void list_sortsAssetsByNameAscendingWhenRequested() throws Exception {
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Zulu Asset");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Alpha Asset");
    AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Middle Asset");

    mockMvc.perform(get("/api/assets")
        .with(authenticated())
        .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].name").value("Alpha Asset"))
        .andExpect(jsonPath("$.content[1].name").value("Middle Asset"))
        .andExpect(jsonPath("$.content[2].name").value("Zulu Asset"));
  }

  @Test
  void getById_returns200_andBody() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);

    mockMvc.perform(get("/api/assets/{id}", assetId)
        .with(authenticated()))
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

    mockMvc.perform(get("/api/assets/{id}", missingAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void getById_returns404_whenAssetBelongsToDifferentTenant() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        AssetFixtures.validCreateRequest("other-tenant", "Other Tenant Asset"));

    mockMvc.perform(get("/api/assets/{id}", assetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Asset not found: " + assetId));
  }

  @Test
  void update_returns200_andUpdatesAsset() throws Exception {
    String createdBy = "asset-creator";
    String updatedBy = "asset-editor";

    String createResponse = mockMvc.perform(post("/api/assets")
        .with(authenticated(createdBy))
        .contentType(MediaType.APPLICATION_JSON)
        .content(AssetFixtures.validCreateRequestJson(objectMapper)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID assetId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

    mockMvc.perform(patch("/api/assets/{id}", assetId)
        .with(authenticated(updatedBy))
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
        .andExpect(jsonPath("$.location").value("eu-central-1"))
        .andExpect(jsonPath("$.createdBy").value(createdBy))
        .andExpect(jsonPath("$.updatedBy").value(updatedBy));

    var saved = assetRepository.findById(assetId);
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getName()).isEqualTo("Updated Gateway Asset");
    assertThat(saved.orElseThrow().getEnvironment()).isEqualTo("staging");
    assertThat(saved.orElseThrow().getHostname()).isEqualTo("gateway-02.example.com");
    assertThat(saved.orElseThrow().getLocation()).isEqualTo("eu-central-1");
    assertThat(saved.orElseThrow().getCreatedBy()).isEqualTo(createdBy);
    assertThat(saved.orElseThrow().getUpdatedBy()).isEqualTo(updatedBy);
  }

  @Test
  void update_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(patch("/api/assets/{id}", missingAssetId)
        .with(authenticated())
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
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);

    mockMvc.perform(delete("/api/assets/{id}", assetId)
        .with(adminAuthenticated()))
        .andExpect(status().isNoContent());

    assertThat(assetRepository.existsById(assetId)).isFalse();
  }

  @Test
  void delete_returns403_whenCallerLacksAdminRole() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);

    mockMvc.perform(delete("/api/assets/{id}", assetId)
        .with(authenticated()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message").value("Access denied"))
        .andExpect(jsonPath("$.path").value("/api/assets/" + assetId));

    assertThat(assetRepository.existsById(assetId)).isTrue();
  }

  @Test
  void delete_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(delete("/api/assets/{id}", missingAssetId)
        .with(adminAuthenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void listBindingsByAssetId_returns200_andOnlyBindingsForAsset() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID otherAssetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Broker Asset");

    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID otherCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Gateway Client Certificate");
    UUID unrelatedCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Unrelated Certificate");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetId,
        BindingType.MQTT_ENDPOINT,
        "mqtt.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        otherCertificateId,
        assetId,
        BindingType.HTTPS_ENDPOINT,
        "https://broker.example.com",
        443);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        unrelatedCertificateId,
        otherAssetId,
        BindingType.DEVICE_CERT,
        null,
        null);

    mockMvc.perform(get("/api/assets/{assetId}/bindings", assetId)
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].assetId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(assetId.toString()))))
        .andExpect(jsonPath("$.content[*].certificateId",
            org.hamcrest.Matchers.containsInAnyOrder(
                certificateId.toString(),
                otherCertificateId.toString())))
        .andExpect(jsonPath("$.content[*].certificateName",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Broker TLS Certificate",
                "Gateway Client Certificate")))
        .andExpect(jsonPath("$.content[*].bindingType",
            org.hamcrest.Matchers.containsInAnyOrder(
                "MQTT_ENDPOINT",
                "HTTPS_ENDPOINT")));
  }

  @Test
  void listBindingsByAssetId_supportsPaginationAndSorting() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID certificateAId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate A");
    UUID certificateBId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate B");
    UUID certificateCId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate C");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateCId,
        assetId,
        BindingType.MQTT_ENDPOINT,
        "charlie.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateAId,
        assetId,
        BindingType.MQTT_ENDPOINT,
        "alpha.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateBId,
        assetId,
        BindingType.MQTT_ENDPOINT,
        "bravo.example.com",
        8883);

    mockMvc.perform(get("/api/assets/{assetId}/bindings", assetId)
        .with(authenticated())
        .param("page", "0")
        .param("size", "2")
        .param("sort", "endpoint,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].endpoint").value("alpha.example.com"))
        .andExpect(jsonPath("$.content[1].endpoint").value("bravo.example.com"));

    mockMvc.perform(get("/api/assets/{assetId}/bindings", assetId)
        .with(authenticated())
        .param("page", "1")
        .param("size", "2")
        .param("sort", "endpoint,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].endpoint").value("charlie.example.com"));
  }

  @Test
  void listBindingsByAssetId_returns404_whenAssetDoesNotExist() throws Exception {
    UUID missingAssetId = UUID.randomUUID();

    mockMvc.perform(get("/api/assets/{assetId}/bindings", missingAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId + "/bindings"));
  }

  @Test
  void listCertificatesByAssetId_returns200_andOnlyCertificatesForAsset() throws Exception {
    UUID assetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID otherAssetId = AssetFixtures.createAndReturnId(mockMvc, objectMapper, "demo-tenant", "Broker Asset");

    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID otherCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Gateway Client Certificate");
    UUID unrelatedCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Unrelated Certificate");

    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        certificateId,
        assetId,
        BindingType.MQTT_ENDPOINT,
        "mqtt.example.com",
        8883);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        otherCertificateId,
        assetId,
        BindingType.HTTPS_ENDPOINT,
        "https://broker.example.com",
        443);
    CertificateBindingFixtures.create(
        mockMvc,
        objectMapper,
        unrelatedCertificateId,
        otherAssetId,
        BindingType.DEVICE_CERT,
        null,
        null);

    mockMvc.perform(get("/api/assets/{assetId}/certificates", assetId)
        .with(authenticated()))
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

    mockMvc.perform(get("/api/assets/{assetId}/certificates", missingAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId + "/certificates"));
  }
}
