package com.combotto.controlplane.support;

import java.util.UUID;

import com.combotto.controlplane.api.CreateCertificateBindingRequest;
import com.combotto.controlplane.model.BindingType;

import tools.jackson.databind.ObjectMapper;

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
}
