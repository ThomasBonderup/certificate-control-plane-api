package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asset record returned by the control plane")
public record AssetResponse(
                @Schema(description = "Unique asset identifier.", example = "11111111-1111-1111-1111-111111111111") UUID id,
                @Schema(description = "Owning tenant identifier.", example = "demo-tenant") String tenantId,
                @Schema(description = "Display name for the asset.", example = "Primary MQTT Broker") String name,
                @Schema(description = "Asset type rendered as a string enum.", example = "BROKER") String assetType,
                @Schema(description = "Deployment environment or ring.", example = "prod-eu-west") String environment,
                @Schema(description = "Hostname for the asset.", example = "broker-01.example.com") String hostname,
                @Schema(description = "Physical or logical location.", example = "eu-west-1") String location,
                @Schema(description = "Creation timestamp.", example = "2026-04-20T12:00:00Z") OffsetDateTime createdAt,
                @Schema(description = "Last update timestamp.", example = "2026-04-20T12:05:00Z") OffsetDateTime updatedAt,
                @Schema(description = "User or service that created the record.", example = "demo-writer") String createdBy,
                @Schema(description = "User or service that last updated the record.", example = "demo-writer") String updatedBy) {
}
