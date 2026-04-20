package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AssetType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partial update payload for an asset")
public record UpdateAssetRequest(
    @Schema(description = "Updated display name for the asset.", example = "Primary MQTT Broker") String name,
    @Schema(description = "Updated operational asset classification.", example = "BROKER") AssetType assetType,
    @Schema(description = "Updated deployment environment or ring.", example = "prod-eu-west") String environment,
    @Schema(description = "Updated hostname.", example = "broker-01.example.com") String hostname,
    @Schema(description = "Updated physical or logical location.", example = "eu-west-1") String location) {
}
