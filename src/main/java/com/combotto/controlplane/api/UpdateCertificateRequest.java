package com.combotto.controlplane.api;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

import java.time.OffsetDateTime;

public record UpdateCertificateRequest(
  String name,
  String commonName,
  String issuer,
  String serialNumber,
  String sha256Fingerprint,
  OffsetDateTime notBefore,
  OffsetDateTime notAfter,
  CertificateStatus status,
  RenewalStatus renewalStatus,
  String blockedReason,
  String owner,
  String notes
) {}
