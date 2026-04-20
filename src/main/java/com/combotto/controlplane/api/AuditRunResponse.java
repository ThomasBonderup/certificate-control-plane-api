package com.combotto.controlplane.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Audit run status returned by the audit endpoints")
public record AuditRunResponse(
    @Schema(description = "Audit run identifier.", example = "42") long id,
    @Schema(description = "Asset identifier associated with the run.", example = "asset-broker-01") String assetId,
    @Schema(description = "Requested audit profile.", example = "tls-baseline") String profile,
    @Schema(description = "Current audit run status.", example = "QUEUED") String status) {
}
