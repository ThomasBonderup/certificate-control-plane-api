package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.combotto.controlplane.model.BindingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Binding between a certificate and an asset endpoint or usage")
public record CertificateBindingResponse(
    @Schema(description = "Unique binding identifier.", example = "33333333-3333-3333-3333-333333333333")
    UUID id,
    @Schema(description = "Associated certificate identifier.", example = "22222222-2222-2222-2222-222222222222")
    UUID certificateId,
    @Schema(description = "Certificate display name.", example = "Broker TLS Certificate")
    String certificateName,
    @Schema(description = "Associated Combotto Monitor asset identifier.", example = "7")
    Long assetId,
    @Schema(description = "Asset display name.", example = "Primary MQTT Broker")
    String assetName,
    @Schema(description = "Type of asset-to-certificate relationship.", example = "BROKER_ENDPOINT")
    BindingType bindingType,
    @Schema(description = "Bound endpoint or hostname, when relevant.", example = "mqtt.example.com")
    String endpoint,
    @Schema(description = "Bound network port, when relevant.", example = "8883")
    Integer port,
    @Schema(description = "Binding creation timestamp.", example = "2026-04-20T12:00:00Z")
    OffsetDateTime createdAt) {
}
