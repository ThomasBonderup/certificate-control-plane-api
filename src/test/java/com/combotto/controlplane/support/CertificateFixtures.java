package com.combotto.controlplane.support;

import java.time.OffsetDateTime;

import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

import tools.jackson.databind.ObjectMapper;

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
}
