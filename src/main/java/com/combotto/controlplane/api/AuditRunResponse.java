package com.combotto.controlplane.api;

public record AuditRunResponse(
    long id,
    String assetId,
    String profile,
    String status) {
}
