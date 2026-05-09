package com.combotto.controlplane.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificate_renewal_status_history", schema = "control_plane")
public class CertificateRenewalStatusHistoryEntity {

  @Id
  private UUID id;

  @Column(name = "certificate_id", nullable = false)
  private UUID certificateId;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "old_renewal_status")
  private String oldRenewalStatus;

  @Column(name = "new_renewal_status", nullable = false)
  private String newRenewalStatus;

  @Column(name = "blocked_reason", columnDefinition = "text")
  private String blockedReason;

  @Column(name = "updated_by")
  private String updatedBy;

  @Column(name = "occurred_at", nullable = false)
  private OffsetDateTime occurredAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCertificateId() {
    return certificateId;
  }

  public void setCertificateId(UUID certificateId) {
    this.certificateId = certificateId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getOldRenewalStatus() {
    return oldRenewalStatus;
  }

  public void setOldRenewalStatus(String oldRenewalStatus) {
    this.oldRenewalStatus = oldRenewalStatus;
  }

  public String getNewRenewalStatus() {
    return newRenewalStatus;
  }

  public void setNewRenewalStatus(String newRenewalStatus) {
    this.newRenewalStatus = newRenewalStatus;
  }

  public String getBlockedReason() {
    return blockedReason;
  }

  public void setBlockedReason(String blockedReason) {
    this.blockedReason = blockedReason;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
