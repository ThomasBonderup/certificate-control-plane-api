package com.combotto.controlplane.services;

import com.combotto.controlplane.model.EvidenceEnvelope;

public interface EvidencePublisher {
  void publish(EvidenceEnvelope envelope);
}
