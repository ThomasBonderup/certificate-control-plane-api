package com.combotto.controlplane.support;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.model.AssetType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.combotto.controlplane.support.SecurityTestSupport.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AssetFixtures {

  private AssetFixtures() {
  }

  public static CreateAssetRequest validCreateRequest() {
    return validCreateRequest(
        "demo-tenant",
        "Primary Gateway Asset",
        AssetType.GATEWAY,
        "production",
        "gateway-01.example.com",
        "eu-west-1");
  }

  public static CreateAssetRequest validCreateRequest(
      String tenantId,
      String name) {
    return validCreateRequest(
        tenantId,
        name,
        AssetType.GATEWAY,
        "production",
        slugify(name) + ".example.com",
        "eu-west-1");
  }

  public static CreateAssetRequest validCreateRequest(
      String tenantId,
      String name,
      AssetType assetType,
      String environment,
      String hostname,
      String location) {
    return new CreateAssetRequest(
        tenantId,
        name,
        assetType,
        environment,
        hostname,
        location);
  }

  public static String validCreateRequestJson(ObjectMapper objectMapper) throws Exception {
    return validCreateRequestJson(objectMapper, validCreateRequest());
  }

  public static String validCreateRequestJson(
      ObjectMapper objectMapper,
      CreateAssetRequest request) throws Exception {
    return objectMapper.writeValueAsString(request);
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper) throws Exception {
    return createAndReturnId(mockMvc, objectMapper, validCreateRequest());
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      String tenantId,
      String name) throws Exception {
    return createAndReturnId(mockMvc, objectMapper, validCreateRequest(tenantId, name));
  }

  public static UUID createAndReturnId(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      CreateAssetRequest request) throws Exception {
    String responseBody = mockMvc.perform(post("/api/assets")
        .with(authenticated("test-user", request.tenantId()))
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
