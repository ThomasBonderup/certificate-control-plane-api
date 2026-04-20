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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/evidence")
@Tag(name = "Evidence", description = "Ingest raw evidence from probes and collectors")
public class EvidenceIngestController {

  private final EvidenceIngestService ingestService;

  public EvidenceIngestController(EvidenceIngestService ingestService) {
    this.ingestService = ingestService;
  }

  @Operation(summary = "Ingest evidence", description = "Accepts a raw evidence payload and forwards it into the ingest pipeline")
  @ApiResponse(responseCode = "202", description = "Evidence accepted for ingestion")
  @PostMapping
  public ResponseEntity<Void> ingest(@RequestBody IngestEvidenceRequest req) {
    System.out.println("HIT EvidenceIngestController: " + req);
    long auditRunId = 123L;
    EvidenceEnvelope envelope = EvidenceMapper.toEnvelope(req, auditRunId);
    ingestService.ingest(envelope);
    return ResponseEntity.accepted().build();
  }

}
