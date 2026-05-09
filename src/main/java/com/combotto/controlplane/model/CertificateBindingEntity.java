package com.combotto.controlplane.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "certificate_bindings", schema = "control_plane", uniqueConstraints = @UniqueConstraint(name = "uq_certificate_asset_binding", columnNames = {
    "certificate_id", "asset_id", "binding_type", "endpoint", "port"
}))
public class CertificateBindingEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "certificate_id", nullable = false)
  private CertificateEntity certificate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private AssetEntity asset;

  @Enumerated(EnumType.STRING)
  @Column(name = "binding_type", nullable = false)
  private BindingType bindingType;

  private String endpoint;

  private Integer port;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public CertificateEntity getCertificate() {
    return certificate;
  }

  public void setCertificate(CertificateEntity certificate) {
    this.certificate = certificate;
  }

  public AssetEntity getAsset() {
    return asset;
  }

  public void setAsset(AssetEntity asset) {
    this.asset = asset;
  }

  public BindingType getBindingType() {
    return bindingType;
  }

  public void setBindingType(BindingType bindingType) {
    this.bindingType = bindingType;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
