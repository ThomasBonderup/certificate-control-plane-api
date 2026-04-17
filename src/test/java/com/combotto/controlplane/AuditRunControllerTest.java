package com.combotto.controlplane;

import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.combotto.controlplane.controller.AuditRunController;
import com.combotto.controlplane.model.AuditRun;
import com.combotto.controlplane.services.AuditRunService;

@WebMvcTest(
    value = AuditRunController.class,
    excludeAutoConfiguration = {
        OAuth2ResourceServerAutoConfiguration.class
    })
public class AuditRunControllerTest {

  @Autowired
  MockMvc mvc;

  @MockitoBean
  AuditRunService auditRunService;

  @Test
  void create_returns201_andLocation_andBody() throws Exception {
    AuditRun created = new AuditRun(1L, "gw-123", "mqtt-tls", "QUEUED");
    when(auditRunService.create(org.mockito.ArgumentMatchers.any()))
        .thenReturn(created);

    mvc.perform(post("/audit/runs")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"assetId":"gw-123","profile":"mqtt-tls"}
            """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("audit/runs/1")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.assetId").value("gw-123"))
        .andExpect(jsonPath("$.profile").value("mqtt-tls"))
        .andExpect(jsonPath("$.status").value("QUEUED"));
  }

  @Test
  void getById_returns200_andBody() throws Exception {
    AuditRun run = new AuditRun(1L, "gw-123", "mqtt-tls", "QUEUED");
    when(auditRunService.getById(1L)).thenReturn(run);

    mvc.perform(get("/audit/runs/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.assetId").value("gw-123"))
        .andExpect(jsonPath("$.profile").value("mqtt-tls"))
        .andExpect(jsonPath("$.status").value("QUEUED"));
  }
}
