package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.validation.Valid;

import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CertificateSummaryResponse;
import com.combotto.controlplane.api.CreateCertificateRequest;
import com.combotto.controlplane.api.UpdateCertificateRequest;
import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;
import com.combotto.controlplane.services.CertificateService;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

  private final CertificateService certificateService;

  public CertificateController(CertificateService certificateService) {
    this.certificateService = certificateService;
  }

  @PostMapping
  public ResponseEntity<CertificateResponse> create(
      @Valid @RequestBody CreateCertificateRequest request,
      UriComponentsBuilder uriBuilder) {

    CertificateResponse created = certificateService.create(request);

    URI location = uriBuilder
        .path("/api/certificates/{id}")
        .buildAndExpand(created.id())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  public Page<CertificateResponse> list(
      @RequestParam(required = false) String tenantId,
      @RequestParam(required = false) CertificateStatus status,
      @RequestParam(required = false) RenewalStatus renewalStatus,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return certificateService.list(tenantId, status, renewalStatus, pageable);
  }

  @GetMapping("/expiring-soon")
  public Page<CertificateResponse> listExpiringSoon(
      @RequestParam(defaultValue = "30") int days,
      @RequestParam(required = false) String tenantId,
      @RequestParam(required = false) String owner,
      @RequestParam(required = false) RenewalStatus renewalStatus,
      @PageableDefault(size = 20, sort = "notAfter", direction = Sort.Direction.ASC) Pageable pageable) {
    return certificateService.listExpiringSoon(days, tenantId, owner, renewalStatus, pageable);
  }

  @GetMapping("/attention-needed")
  public Page<CertificateResponse> listAttentionNeeded(
      @RequestParam(defaultValue = "30") int days,
      @PageableDefault(size = 20, sort = "notAfter", direction = Sort.Direction.ASC) Pageable pageable) {
    return certificateService.listAttentionNeeded(days, pageable);
  }

  @GetMapping("/{id}")
  public CertificateResponse getById(@PathVariable UUID id) {
    return certificateService.getById(id);
  }

  @PatchMapping("/{id}")
  public CertificateResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCertificateRequest request) {
    return certificateService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    certificateService.delete(id);
  }

  @GetMapping("/summary")
  public CertificateSummaryResponse summary() {
    return certificateService.summary();
  }
}
