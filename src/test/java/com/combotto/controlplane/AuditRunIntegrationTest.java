package com.combotto.controlplane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.combotto.controlplane.repositories.AuditRunRepository;
import com.combotto.controlplane.model.AuditRun;
import com.combotto.controlplane.services.AssetService;
import com.combotto.controlplane.services.AuditRunService;
import com.combotto.controlplane.services.CertificateBindingService;
import com.combotto.controlplane.controller.AuditRunController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@WebMvcTest(AuditRunController.class)
@Import({AuditRunService.class, AuditRunIntegrationTest.TestAuditRunConfig.class})
public class AuditRunIntegrationTest {

  @Autowired
  MockMvc mvc;

  @Test
  void create_and_then_get_roundTrip() throws Exception {
    String createJson = """
        {"assetId":"gw-123","profile":"mqtt-tls"}
        """;

    var result = mvc.perform(post("/audit/runs")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createJson))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andReturn();

    String location = result.getResponse().getHeader("Location");

    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assetId").value("gw-123"))
        .andExpect(jsonPath("$.status").value("QUEUED"));
  }

  @TestConfiguration
  static class TestAuditRunConfig {

    @Bean
    AuditRunRepository auditRunRepository() {
      return new InMemoryAuditRunRepository();
    }

    @Bean
    AssetService assetService() {
      return org.mockito.Mockito.mock(AssetService.class);
    }

    @Bean
    CertificateBindingService certificateBindingService() {
      return org.mockito.Mockito.mock(CertificateBindingService.class);
    }
  }

  static class InMemoryAuditRunRepository implements AuditRunRepository {
    private final Map<Long, AuditRun> store = new ConcurrentHashMap<>();

    @Override
    public AuditRun save(AuditRun run) {
      store.put(run.id(), run);
      return run;
    }

    @Override
    public Optional<AuditRun> findById(long id) {
      return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AuditRun> findByAssetId(String assetId) {
      return store.values().stream()
          .filter(run -> run.assetId().equals(assetId))
          .toList();
    }
  }
}
