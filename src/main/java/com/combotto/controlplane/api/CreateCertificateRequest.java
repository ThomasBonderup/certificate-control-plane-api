package com.combotto.controlplane.api;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateCertificateRequest(
        @NotBlank String tenantId,
        @NotBlank String name,
        String commonName,
        String issuer,
        String serialNumber,
        String sha256Fingerprint,
        OffsetDateTime notBefore,
        OffsetDateTime notAfter,
        @NotNull CertificateStatus status,
        @NotNull RenewalStatus renewalStatus,
        String owner,
        String notes
) {}
