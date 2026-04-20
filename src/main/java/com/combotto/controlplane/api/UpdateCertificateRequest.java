package com.combotto.controlplane.api;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Partial update payload for certificate metadata and renewal workflow fields")
public record UpdateCertificateRequest(
  @Schema(description = "Updated display name.", example = "Broker TLS Certificate") String name,
  @Schema(description = "Updated certificate common name.", example = "mqtt.example.com") String commonName,
  @Schema(description = "Updated issuer.", example = "Let's Encrypt") String issuer,
  @Schema(description = "Updated serial number.", example = "123456789") String serialNumber,
  @Schema(description = "Updated SHA-256 fingerprint.", example = "AB:CD:EF:12:34") String sha256Fingerprint,
  @Schema(description = "Updated validity start timestamp.", example = "2026-04-01T00:00:00Z") OffsetDateTime notBefore,
  @Schema(description = "Updated validity end timestamp.", example = "2026-07-01T00:00:00Z") OffsetDateTime notAfter,
  @Schema(description = "Updated lifecycle status.", example = "ACTIVE") CertificateStatus status,
  @Schema(description = "Updated renewal workflow state.", example = "IN_PROGRESS") RenewalStatus renewalStatus,
  @Schema(description = "Reason the renewal is blocked. Required when renewalStatus is BLOCKED.", example = "Waiting for vendor approval") String blockedReason,
  @Schema(description = "Updated owner.", example = "platform-team") String owner,
  @Schema(description = "Updated operator notes.", example = "Renewal has started") String notes
) {}
