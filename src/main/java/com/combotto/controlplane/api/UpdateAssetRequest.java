package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AssetType;

public record UpdateAssetRequest(
    String name,
    AssetType assetType,
    String environment,
    String hostname,
    String location) {
}
