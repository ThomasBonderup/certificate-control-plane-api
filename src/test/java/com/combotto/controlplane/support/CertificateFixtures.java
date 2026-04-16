package com.combotto.controlplane.support;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class CertificateFixtures {

  private CertificateFixtures() {
  }

  public static CreateCertificateRequest validCreateRequest() {
    return validCreateRequest(
        "demo-tenant",
        "Broker TLS Certificate",
        "mqtt.example.com",
        "Let's Encrypt",
        "123456789",
        "AB:CD:EF:12:34",
        OffsetDateTime.parse("2026-04-01T00:00:00Z"),
        OffsetDateTime.parse("2026-07-01T00:00:00Z"),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "first certificate");
  }

  public static CreateCertificateRequest validCreateRequest(String name) {
    return validCreateRequest(
        "demo-tenant",
        name,
        slugify(name) + ".example.com",
        "Let's Encrypt",
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        OffsetDateTime.parse("2026-04-01T00:00:00Z"),
        OffsetDateTime.parse("2026-07-01T00:00:00Z"),
        CertificateStatus.ACTIVE,
        RenewalStatus.NOT_STATUS,
        "thomas",
        "test certificate");
  }

  public static CreateCertificateRequest validCreateRequest(
      String tenantId,
      String name,
      String commonName,
      String issuer,
      String serialNumber,
      String sha256Fingerprint,
      OffsetDateTime notBefore,
      OffsetDateTime notAfter,
      CertificateStatus status,
      RenewalStatus renewalStatus,
      String owner,
      String notes) {
    return new CreateCertificateRequest(
        tenantId,
        name,
        commonName,
        issuer,
        serialNumber,
        sha256Fingerprint,
        notBefore,
        notAfter,
        status,
        renewalStatus,
        owner,
        notes);
  }

  public static String validCreateRequestJson(ObjectMapper objectMapper) throws Exception {
    return objectMapper.writeValueAsString(validCreateRequest());
  }

  public static String validCreateRequestJson(
      ObjectMapper objectMapper,
      CreateCertificateRequest request) throws Exception {
    return objectMapper.writeValueAsString(request);
  }

  public static void create(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      CreateCertificateRequest request) throws Exception {
    mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateRequestJson(objectMapper, request)))
        .andExpect(status().isCreated());
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper) throws Exception {
    return createAndReturnId(mockMvc, objectMapper, validCreateRequest());
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      CreateCertificateRequest request) throws Exception {
    String responseBody = mockMvc.perform(post("/api/certificates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateRequestJson(objectMapper, request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    return UUID.fromString(body.get("id").asString());
  }

  private static String slugify(String value) {
    return value.toLowerCase().replace(" ", "-");
  }
}
