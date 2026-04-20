package com.combotto.controlplane.api;

import java.util.UUID;

import com.combotto.controlplane.model.BindingType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload used to bind a certificate to an asset or endpoint")
public record CreateCertificateBindingRequest(
    @Schema(description = "Asset identifier that the certificate should be bound to.", example = "11111111-1111-1111-1111-111111111111")
    @NotNull
    UUID assetId,
    @Schema(description = "Type of binding to create.", example = "BROKER_ENDPOINT")
    @NotNull
    BindingType bindingType,
    @Schema(description = "Endpoint or hostname associated with the binding.", example = "mqtt.example.com")
    String endpoint,
    @Schema(description = "Network port associated with the binding.", example = "8883")
    Integer port) {
}
