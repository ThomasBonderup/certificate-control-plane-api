package com.combotto.controlplane.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tenant-level certificate summary metrics")
public record CertificateSummaryResponse(
    @Schema(description = "Total certificates visible to the tenant.", example = "12")
    long total,
    @Schema(description = "Certificates currently marked ACTIVE.", example = "8")
    long active,
    @Schema(description = "Certificates expiring within the default summary window.", example = "3")
    long expiredSoon,
    @Schema(description = "Certificates already expired.", example = "1")
    long expired,
    @Schema(description = "Certificates currently in renewal progress.", example = "2")
    long renewalProgress) {
}
