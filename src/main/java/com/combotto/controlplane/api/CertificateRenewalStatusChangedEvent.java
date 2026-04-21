package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CertificateRenewalStatusChangedEvent(
    UUID certificateId,
    String tenantId,
    String oldRenewalStatus,
    String newRenewalStatus,
    String blockedReason,
    OffsetDateTime renewalUpdatedAt,
    String updatedBy,
    OffsetDateTime occurredAt) {
}
