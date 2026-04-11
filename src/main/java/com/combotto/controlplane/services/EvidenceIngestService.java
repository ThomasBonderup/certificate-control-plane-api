package com.combotto.controlplane.services;

import com.combotto.controlplane.model.EvidenceEnvelope;

public interface EvidenceIngestService {
  void ingest(EvidenceEnvelope envelope);
}
