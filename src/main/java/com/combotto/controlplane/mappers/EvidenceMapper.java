package com.combotto.controlplane.mappers;

import java.time.Instant;

import com.combotto.controlplane.api.IngestEvidenceRequest;
import com.combotto.controlplane.model.EvidenceEnvelope;

public final class EvidenceMapper {
  private EvidenceMapper() {
  }

  public static EvidenceEnvelope toEnvelope(IngestEvidenceRequest req, long auditRunId) {
    return new EvidenceEnvelope(
        0L,
        req.assetId(),
        req.probe(),
        req.collectedAt() != null ? req.collectedAt() : Instant.now(),
        req.rawJson(),
        auditRunId);
  }
}
