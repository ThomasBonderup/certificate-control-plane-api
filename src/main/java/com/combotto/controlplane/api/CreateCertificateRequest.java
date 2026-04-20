package com.combotto.controlplane.api;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "Payload used to register a certificate in the authenticated tenant")
public record CreateCertificateRequest(
                @Schema(description = "Tenant identifier. Must match the tenant in the bearer token.", example = "demo-tenant")
                @NotBlank
                String tenantId,
                @Schema(description = "Display name for the certificate record.", example = "Broker TLS Certificate")
                @NotBlank
                String name,
                @Schema(description = "Common name embedded in the certificate.", example = "mqtt.example.com")
                String commonName,
                @Schema(description = "Issuer that signed the certificate.", example = "Let's Encrypt")
                String issuer,
                @Schema(description = "Certificate serial number.", example = "123456789")
                String serialNumber,
                @Schema(description = "SHA-256 fingerprint for de-duplication and lookup.", example = "AB:CD:EF:12:34")
                String sha256Fingerprint,
                @Schema(description = "Validity start timestamp in ISO-8601 format.", example = "2026-04-01T00:00:00Z")
                OffsetDateTime notBefore,
                @Schema(description = "Validity end timestamp in ISO-8601 format.", example = "2026-07-01T00:00:00Z")
                OffsetDateTime notAfter,
                @Schema(description = "Current observed certificate lifecycle status.", example = "ACTIVE")
                @NotNull
                CertificateStatus status,
                @Schema(description = "Current renewal workflow state.", example = "NOT_STATUS")
                @NotNull
                RenewalStatus renewalStatus,
                @Schema(description = "Human or team responsible for the certificate.", example = "platform-team")
                String owner,
                @Schema(description = "Free-form notes for operations or renewal context.", example = "Primary broker certificate")
                String notes) {
}
