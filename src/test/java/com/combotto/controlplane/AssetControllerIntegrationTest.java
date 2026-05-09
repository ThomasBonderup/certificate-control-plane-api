package com.combotto.controlplane;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerIntegrationTest {

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
  void list_returns200_andActiveCombottoAssets() throws Exception {
    AssetFixtures.createAndReturnId(assetRepository, 1001L, "Primary Gateway Asset");
    AssetFixtures.createAndReturnId(assetRepository, 1001L, "Broker Asset");
    AssetFixtures.createDeletedAndReturnId(assetRepository, "Deleted Asset");

    mockMvc.perform(get("/api/assets")
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].name",
            org.hamcrest.Matchers.containsInAnyOrder(
                "Primary Gateway Asset",
                "Broker Asset")))
        .andExpect(jsonPath("$.content[*].companyId",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(1001))))
        .andExpect(jsonPath("$.content[*].assetType",
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("gateway"))));
  }

  @Test
  void list_respectsRequestedPageSizeAndSort() throws Exception {
    AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset C");
    AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset A");
    AssetFixtures.createAndReturnId(assetRepository, 1001L, "Asset B");

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
  void getById_returns200_andCombottoAssetBody() throws Exception {
    Long assetId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Primary Gateway Asset");

    mockMvc.perform(get("/api/assets/{id}", assetId)
        .with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId.intValue()))
        .andExpect(jsonPath("$.companyId").value(1001))
        .andExpect(jsonPath("$.name").value("Primary Gateway Asset"))
        .andExpect(jsonPath("$.assetType").value("gateway"))
        .andExpect(jsonPath("$.externalRef").value("mqtt://primary-gateway-asset.example.com:1883"))
        .andExpect(jsonPath("$.protocol").value("mqtt"))
        .andExpect(jsonPath("$.siteLabel").value("lab"))
        .andExpect(jsonPath("$.deleted").value(false));
  }

  @Test
  void getById_returns404_whenAssetDoesNotExist() throws Exception {
    Long missingAssetId = 9999L;

    mockMvc.perform(get("/api/assets/{id}", missingAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId));
  }

  @Test
  void getById_returns404_whenAssetIsSoftDeleted() throws Exception {
    Long deletedAssetId = AssetFixtures.createDeletedAndReturnId(assetRepository, "Deleted Asset");

    mockMvc.perform(get("/api/assets/{id}", deletedAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Asset not found: " + deletedAssetId));
  }

  @Test
  void listBindingsByAssetId_returns200_andOnlyBindingsForAsset() throws Exception {
    Long assetId = AssetFixtures.createAndReturnId(assetRepository);
    Long otherAssetId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Broker Asset");

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
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(assetId.intValue()))))
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
    Long assetId = AssetFixtures.createAndReturnId(assetRepository);
    UUID certificateAId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate A");
    UUID certificateBId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate B");
    UUID certificateCId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper, "Certificate C");

    CertificateBindingFixtures.create(mockMvc, objectMapper, certificateCId, assetId, BindingType.MQTT_ENDPOINT, "charlie.example.com", 8883);
    CertificateBindingFixtures.create(mockMvc, objectMapper, certificateAId, assetId, BindingType.MQTT_ENDPOINT, "alpha.example.com", 8883);
    CertificateBindingFixtures.create(mockMvc, objectMapper, certificateBId, assetId, BindingType.MQTT_ENDPOINT, "bravo.example.com", 8883);

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
  }

  @Test
  void listBindingsByAssetId_returns404_whenAssetDoesNotExist() throws Exception {
    Long missingAssetId = 9999L;

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
    Long assetId = AssetFixtures.createAndReturnId(assetRepository);
    Long otherAssetId = AssetFixtures.createAndReturnId(assetRepository, 1001L, "Broker Asset");

    UUID certificateId = CertificateFixtures.createAndReturnId(mockMvc, objectMapper);
    UUID otherCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Gateway Client Certificate");
    UUID unrelatedCertificateId = CertificateFixtures.createAndReturnId(
        mockMvc,
        objectMapper,
        "Unrelated Certificate");

    CertificateBindingFixtures.create(mockMvc, objectMapper, certificateId, assetId, BindingType.MQTT_ENDPOINT, "mqtt.example.com", 8883);
    CertificateBindingFixtures.create(mockMvc, objectMapper, otherCertificateId, assetId, BindingType.HTTPS_ENDPOINT, "https://broker.example.com", 443);
    CertificateBindingFixtures.create(mockMvc, objectMapper, unrelatedCertificateId, otherAssetId, BindingType.DEVICE_CERT, null, null);

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
    Long missingAssetId = 9999L;

    mockMvc.perform(get("/api/assets/{assetId}/certificates", missingAssetId)
        .with(authenticated()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Asset not found: " + missingAssetId))
        .andExpect(jsonPath("$.path").value("/api/assets/" + missingAssetId + "/certificates"));
  }
}
