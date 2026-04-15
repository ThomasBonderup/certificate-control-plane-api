package com.combotto.controlplane.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.combotto.controlplane.api.CertificateBindingResponse;
import com.combotto.controlplane.api.CreateCertificateBindingRequest;
import com.combotto.controlplane.services.CertificateBindingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/certificates/{certificateId}/bindings")
public class CertificateBindingController {
  private final CertificateBindingService certificateBindingService;

  public CertificateBindingController(CertificateBindingService certificateBindingService) {
    this.certificateBindingService = certificateBindingService;
  }

  @PostMapping
  public ResponseEntity<CertificateBindingResponse> create(
      @PathVariable UUID certificateId,
      @Valid @RequestBody CreateCertificateBindingRequest request,
      UriComponentsBuilder uriBuilder) {
    CertificateBindingResponse created = certificateBindingService.create(certificateId, request);

    URI location = uriBuilder
        .path("/api/certificates/{certificateId}/bindings")
        .buildAndExpand(certificateId, created.id())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  public List<CertificateBindingResponse> listByCertificateId(@PathVariable UUID certificateId) {
    return certificateBindingService.listByCertificateId(certificateId);
  }
}
