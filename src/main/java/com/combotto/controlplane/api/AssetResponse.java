package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetResponse(
                UUID id,
                String tenantId,
                String name,
                String assetType,
                String environment,
                String hostname,
                String location,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt,
                String createdBy,
                String updatedBy) {
}
