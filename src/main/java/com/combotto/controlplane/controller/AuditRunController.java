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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/audit/runs")
@Tag(name = "Audit Runs", description = "Create and inspect audit runs for the audit pipeline")
public class AuditRunController {

  private final AuditRunService auditRunService;

  public AuditRunController(AuditRunService auditRunService) {
    this.auditRunService = auditRunService;
  }

  @Operation(summary = "Create audit run", description = "Queues a new audit run for an asset and profile")
  @ApiResponse(responseCode = "201", description = "Audit run created")
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

  @Operation(summary = "Get audit run", description = "Returns a single audit run by identifier")
  @ApiResponse(responseCode = "200", description = "Audit run returned successfully")
  @GetMapping("/{id}")
  public AuditRunResponse getById(@Parameter(description = "Audit run identifier.", example = "42") @PathVariable long id) {
    AuditRun run = auditRunService.getById(id);
    return AuditRunMapper.toResponse(run);
  }

  @Operation(summary = "List audit runs by asset", description = "Returns all audit runs associated with an asset identifier")
  @ApiResponse(responseCode = "200", description = "Audit runs returned successfully")
  @GetMapping("/asset/{assetId}")
  public List<AuditRunResponse> getByAssetId(
      @Parameter(description = "Opaque asset identifier from the audit domain.", example = "asset-broker-01") @PathVariable String assetId) {
    return auditRunService.getByAssetId(assetId).stream()
        .map(AuditRunMapper::toResponse)
        .toList();
  }
}
