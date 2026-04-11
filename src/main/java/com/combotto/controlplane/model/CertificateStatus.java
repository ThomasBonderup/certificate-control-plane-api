package com.combotto.controlplane.model;

public enum CertificateStatus {
  ACTIVE,
  EXPIRING_SOON,
  EXPIRED,
  REVOKED,
  REPLACED,
  UNKNOWN
}
