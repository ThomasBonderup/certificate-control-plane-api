package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AssetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload used to register an asset in the authenticated tenant")
public record CreateAssetRequest(
    @Schema(description = "Tenant identifier. Must match the tenant in the bearer token.", example = "demo-tenant") @NotBlank String tenantId,
    @Schema(description = "Display name for the asset.", example = "Primary MQTT Broker") @NotBlank String name,
    @Schema(description = "Operational asset classification.", example = "BROKER") @NotNull AssetType assetType,
    @Schema(description = "Deployment environment or ring.", example = "prod-eu-west") String environment,
    @Schema(description = "Network hostname for the asset, when available.", example = "broker-01.example.com") String hostname,
    @Schema(description = "Physical or logical location.", example = "eu-west-1") String location) {
}
