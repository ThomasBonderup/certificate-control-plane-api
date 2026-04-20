package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Certificate record returned by the control plane")
public record CertificateResponse(
    @Schema(description = "Unique certificate identifier.", example = "22222222-2222-2222-2222-222222222222")
    UUID id,
    @Schema(description = "Owning tenant identifier.", example = "demo-tenant")
    String tenantId,
    @Schema(description = "Display name for the certificate.", example = "Broker TLS Certificate")
    String name,
    @Schema(description = "Common name embedded in the certificate.", example = "mqtt.example.com")
    String commonName,
    @Schema(description = "Certificate issuer.", example = "Let's Encrypt")
    String issuer,
    @Schema(description = "Certificate serial number.", example = "123456789")
    String serialNumber,
    @Schema(description = "SHA-256 fingerprint.", example = "AB:CD:EF:12:34")
    String sha256Fingerprint,
    @Schema(description = "Validity start timestamp.", example = "2026-04-01T00:00:00Z")
    OffsetDateTime notBefore,
    @Schema(description = "Validity end timestamp.", example = "2026-07-01T00:00:00Z")
    OffsetDateTime notAfter,
    @Schema(description = "Current observed lifecycle status.", example = "ACTIVE")
    String status,
    @Schema(description = "Current renewal workflow state.", example = "PLANNED")
    String renewalStatus,
    @Schema(description = "Reason the renewal is blocked, when applicable.", example = "Waiting for maintenance window")
    String blockedReason,
    @Schema(description = "Timestamp of the latest renewal workflow update.", example = "2026-04-20T12:10:00Z")
    OffsetDateTime renewalUpdatedAt,
    @Schema(description = "Human or team responsible for the certificate.", example = "platform-team")
    String owner,
    @Schema(description = "Free-form notes.", example = "Primary broker certificate")
    String notes,
    @Schema(description = "Creation timestamp.", example = "2026-04-20T12:00:00Z")
    OffsetDateTime createdAt,
    @Schema(description = "Last update timestamp.", example = "2026-04-20T12:05:00Z")
    OffsetDateTime updatedAt,
    @Schema(description = "User or service that created the record.", example = "demo-writer")
    String createdBy,
    @Schema(description = "User or service that last updated the record.", example = "demo-writer")
    String updatedBy) {
}
