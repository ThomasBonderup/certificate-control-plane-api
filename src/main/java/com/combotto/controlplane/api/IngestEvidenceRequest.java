package com.combotto.controlplane.api;

import java.time.Instant;

public record IngestEvidenceRequest(
    String assetId,
    String probe,
    Instant collectedAt,
    String rawJson) {
}
