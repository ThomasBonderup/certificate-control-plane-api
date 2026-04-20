package com.combotto.controlplane.api;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload used to ingest raw evidence produced by an audit probe")
public record IngestEvidenceRequest(
    @Schema(description = "Asset identifier that produced the evidence.", example = "asset-broker-01") String assetId,
    @Schema(description = "Probe or collector name.", example = "tls-handshake") String probe,
    @Schema(description = "Timestamp when evidence was collected.", example = "2026-04-20T12:00:00Z") Instant collectedAt,
    @Schema(description = "Raw evidence payload serialized as JSON text.", example = "{\"protocol\":\"TLSv1.3\"}") String rawJson) {
}
