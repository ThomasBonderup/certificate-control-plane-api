package com.combotto.controlplane.model;

import java.time.Instant;

public record EvidenceEnvelope(
    long id,
    String assetId,
    String probe,
    Instant collectedAt,
    String rawJson,
    long auditRunId) {
}
