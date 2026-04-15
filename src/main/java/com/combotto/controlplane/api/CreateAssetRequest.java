package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AssetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssetRequest(
    @NotBlank String tenantId,
    @NotBlank String name,
    @NotNull AssetType assetType,
    String environment,
    String hostname,
    String location) {
}
