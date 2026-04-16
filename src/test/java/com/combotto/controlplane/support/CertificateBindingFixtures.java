package com.combotto.controlplane.support;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.combotto.controlplane.api.CreateCertificateBindingRequest;
import com.combotto.controlplane.model.BindingType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class CertificateBindingFixtures {

  private CertificateBindingFixtures() {
  }

  public static CreateCertificateBindingRequest validCreateRequest(UUID assetId) {
    return validCreateRequest(assetId, BindingType.MQTT_ENDPOINT, "mqtt.example.com", 8883);
  }

  public static CreateCertificateBindingRequest validCreateRequest(
      UUID assetId,
      BindingType bindingType,
      String endpoint,
      Integer port) {
    return new CreateCertificateBindingRequest(assetId, bindingType, endpoint, port);
  }

  public static String validCreateRequestJson(
      ObjectMapper objectMapper,
      UUID assetId) throws Exception {
    return validCreateRequestJson(objectMapper, validCreateRequest(assetId));
  }

  public static String validCreateRequestJson(
      ObjectMapper objectMapper,
      CreateCertificateBindingRequest request) throws Exception {
    return objectMapper.writeValueAsString(request);
  }

  public static void create(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      UUID certificateId,
      CreateCertificateBindingRequest request) throws Exception {
    mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateRequestJson(objectMapper, request)))
        .andExpect(status().isCreated());
  }

  public static void create(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      UUID certificateId,
      UUID assetId,
      BindingType bindingType,
      String endpoint,
      Integer port) throws Exception {
    create(
        mockMvc,
        objectMapper,
        certificateId,
        validCreateRequest(assetId, bindingType, endpoint, port));
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      UUID certificateId,
      UUID assetId) throws Exception {
    return createAndReturnId(mockMvc, objectMapper, certificateId, validCreateRequest(assetId));
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      UUID certificateId,
      CreateCertificateBindingRequest request) throws Exception {
    String responseBody = mockMvc.perform(post("/api/certificates/{certificateId}/bindings", certificateId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateRequestJson(objectMapper, request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode body = objectMapper.readTree(responseBody);
    return UUID.fromString(body.get("id").asString());
  }
}
