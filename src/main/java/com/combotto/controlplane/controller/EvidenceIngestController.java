package com.combotto.controlplane.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.combotto.controlplane.api.IngestEvidenceRequest;
import com.combotto.controlplane.mappers.EvidenceMapper;
import com.combotto.controlplane.model.EvidenceEnvelope;
import com.combotto.controlplane.services.EvidenceIngestService;

@RestController
@RequestMapping("/evidence")
public class EvidenceIngestController {

  private final EvidenceIngestService ingestService;

  public EvidenceIngestController(EvidenceIngestService ingestService) {
    this.ingestService = ingestService;
  }

  @PostMapping
  public ResponseEntity<Void> ingest(@RequestBody IngestEvidenceRequest req) {
    System.out.println("HIT EvidenceIngestController: " + req);
    long auditRunId = 123L;
    EvidenceEnvelope envelope = EvidenceMapper.toEnvelope(req, auditRunId);
    ingestService.ingest(envelope);
    return ResponseEntity.accepted().build();
  }

}
