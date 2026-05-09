package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.util.Map;

public record CertificatePostureSnapshot(
    Map<CertificateStatusRenewalKey, Long> certificateCounts,
    Map<Integer, Long> expiringCountsByWindowDays,
    long expiredCount,
    long blockedRenewalCount,
    long unboundCount,
    Map<String, Long> bindingCountsByType,
    OffsetDateTime nextExpiry) {
}
