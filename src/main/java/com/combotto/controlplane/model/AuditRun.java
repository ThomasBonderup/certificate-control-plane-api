package com.combotto.controlplane.model;

public record AuditRun(
    long id,
    String assetId,
    String profile,
    String status) {
}