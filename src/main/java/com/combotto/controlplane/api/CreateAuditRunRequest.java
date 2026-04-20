package com.combotto.controlplane.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload used to create an audit run for an asset")
public record CreateAuditRunRequest(
    @Schema(description = "Opaque asset identifier from the upstream audit domain.", example = "asset-broker-01")
    String assetId,
    @Schema(description = "Audit profile to execute.", example = "tls-baseline")
    String Profile) {
}
