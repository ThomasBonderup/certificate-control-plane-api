package com.combotto.controlplane.api;

public record CreateAuditRunRequest(
    String assetId,
    String Profile) {
}