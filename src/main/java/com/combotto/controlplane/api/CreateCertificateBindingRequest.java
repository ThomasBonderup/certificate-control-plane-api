package com.combotto.controlplane.api;

import java.util.UUID;

import com.combotto.controlplane.model.BindingType;

import jakarta.validation.constraints.NotNull;

public record CreateCertificateBindingRequest(
    @NotNull UUID assetId,
    @NotNull BindingType bindingType,
    String endpoint,
    Integer port) {
}
