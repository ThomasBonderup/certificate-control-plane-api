package com.combotto.control-plane.model;

public record AuditRun(
    long id,
    String assetId,
    String profile,
    String status) {
}