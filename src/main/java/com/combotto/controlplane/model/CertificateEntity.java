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

  private String notes;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "updated_by")
  private String updatedBy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public String getSha256Fingerprint() {
    return sha256Fingerprint;
  }

  public void setSha256Fingerprint(String sha256Fingerprint) {
    this.sha256Fingerprint = sha256Fingerprint;
  }

  public OffsetDateTime getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(OffsetDateTime notBefore) {
    this.notBefore = notBefore;
  }

  public OffsetDateTime getNotAfter() {
    return notAfter;
  }

  public void setNotAfter(OffsetDateTime notAfter) {
    this.notAfter = notAfter;
  }

  public CertificateStatus getStatus() {
    return status;
  }

  public void setStatus(CertificateStatus status) {
    this.status = status;
  }

  public RenewalStatus getRenewalStatus() {
    return renewalStatus;
  }

  public void setRenewalStatus(RenewalStatus renewalStatus) {
    this.renewalStatus = renewalStatus;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }
}
