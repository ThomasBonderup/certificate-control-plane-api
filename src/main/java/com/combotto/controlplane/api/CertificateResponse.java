package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CertificateResponse(
    UUID id,
    String tenantId,
    String name,
    String commonName,
    String issuer,
    String serialNumber,
    String sha256Fingerprint,
    OffsetDateTime notBefore,
    OffsetDateTime notAfter,
    String status,
    String renewalStatus,
    String blockedReason,
    OffsetDateTime renewalUpdatedAt,
    String owner,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {
}
