package com.combotto.controlplane.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
public class CertificateEntity {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Column(name = "common_name")
  private String commonName;

  private String issuer;

  @Column(name = "serial_number")
  private String serialNumber;

  @Column(name = "sha256_fingerprint")
  private String sha256Fingerprint;

  @Column(name = "not_before")
  private OffsetDateTime notBefore;

  @Column(name = "not_after")
  private OffsetDateTime notAfter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CertificateStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "renewal_status", nullable = false)
  private RenewalStatus renewalStatus;

  private String owner;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
