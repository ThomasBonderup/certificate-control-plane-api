package com.combotto.controlplane.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.combotto.controlplane.model.BindingType;

public record CertificateBindingResponse(
    UUID id,
    UUID certificateId,
    UUID assetId,
    String assetName,
    BindingType bindingType,
    String endpoint,
    Integer port,
    OffsetDateTime createdAt) {
}
