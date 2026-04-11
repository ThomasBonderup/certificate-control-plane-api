package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.combotto.controlplane.api.AuditRunMapper;
import com.combotto.controlplane.api.AuditRunResponse;
import com.combotto.controlplane.api.CreateAuditRunRequest;
import com.combotto.controlplane.model.AuditRun;
import com.combotto.controlplane.services.AuditRunService;

@RestController
@RequestMapping("/audit/runs")
public class AuditRunController {

  private final AuditRunService auditRunService;

  public AuditRunController(AuditRunService auditRunService) {
    this.auditRunService = auditRunService;
  }

  @PostMapping
  public ResponseEntity<AuditRunResponse> create(@RequestBody CreateAuditRunRequest request,
      UriComponentsBuilder uriBuilder) {
    AuditRun created = auditRunService.create(request);
    AuditRunResponse response = AuditRunMapper.toResponse(created);

    URI location = uriBuilder.path("/audit/runs/{id}")
        .buildAndExpand(created.id())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  public AuditRunResponse getById(@PathVariable long id) {
    AuditRun run = auditRunService.getById(id);
    return AuditRunMapper.toResponse(run);
  }

  @GetMapping("/asset/{assetId}")
  public List<AuditRunResponse> getByAssetId(@PathVariable String assetId) {
    return auditRunService.getByAssetId(assetId).stream()
        .map(AuditRunMapper::toResponse)
        .toList();
  }
}
