package com.combotto.controlplane.model;

enum CertificateStatus {
  ACTIVE,
  EXPIRING_SOON,
  EXPIRED,
  REVOKED,
  REPLACED,
  UNKNOWN
}
