package com.combotto.controlplane.support;

import com.combotto.controlplane.api.CreateAssetRequest;
import com.combotto.controlplane.model.AssetType;

import tools.jackson.databind.ObjectMapper;

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
}
