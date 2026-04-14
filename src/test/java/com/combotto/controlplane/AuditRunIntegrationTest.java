package com.combotto.controlplane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.combotto.controlplane.repositories.CertificateRepository;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
public class AuditRunIntegrationTest {

  @Autowired
  MockMvc mvc;

  @MockitoBean
  CertificateRepository certificateRepository;

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
}
