package com.combotto.controlplane.api;

public record CertificateSummaryResponse(
    long total,
    long active,
    long expiredSoon,
    long expired,
    long renewalProgress) {
}
