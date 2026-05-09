package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Renewal workflow history row for a certificate")
public record CertificateRenewalHistoryResponse(
    @Schema(description = "Unique renewal history row identifier.", example = "33333333-3333-3333-3333-333333333333")
    UUID id,
    @Schema(description = "Certificate identifier the history row belongs to.", example = "22222222-2222-2222-2222-222222222222")
    UUID certificateId,
    @Schema(description = "Owning tenant identifier.", example = "demo-tenant")
    String tenantId,
    @Schema(description = "Previous renewal workflow status, when available.", example = "PLANNED")
    String oldRenewalStatus,
    @Schema(description = "New renewal workflow status recorded by this history row.", example = "IN_PROGRESS")
    String newRenewalStatus,
    @Schema(description = "Reason the renewal was blocked, when applicable.", example = "Waiting for maintenance window")
    String blockedReason,
    @Schema(description = "User or service that made the renewal workflow change.", example = "demo-writer")
    String updatedBy,
    @Schema(description = "Timestamp when the renewal workflow change occurred.", example = "2026-04-20T12:10:00Z")
    OffsetDateTime occurredAt,
    @Schema(description = "Timestamp when the history row was stored.", example = "2026-04-20T12:10:01Z")
    OffsetDateTime createdAt) {
}
