package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.combotto.controlplane.api.CertificateBindingResponse;
import com.combotto.controlplane.api.CertificateResponse;
import com.combotto.controlplane.api.CreateCertificateBindingRequest;
import com.combotto.controlplane.common.CertificateMapper;
import com.combotto.controlplane.common.ResourceNotFoundException;
import com.combotto.controlplane.model.AssetEntity;
import com.combotto.controlplane.model.CertificateBindingEntity;
import com.combotto.controlplane.model.CertificateEntity;
import com.combotto.controlplane.repositories.AssetRepository;
import com.combotto.controlplane.repositories.CertificateBindingRepository;
import com.combotto.controlplane.repositories.CertificateRepository;

@Service
public class CertificateBindingService {

  private final CertificateRepository certificateRepository;
  private final AssetRepository assetRepository;
  private final CertificateBindingRepository certificateBindingRepository;
  private final CertificateMapper certificateMapper;

  public CertificateBindingService(
      CertificateRepository certificateRepository,
      AssetRepository assetRepository,
      CertificateBindingRepository certificateBindingRepository,
      CertificateMapper certificateMapper) {
    this.certificateRepository = certificateRepository;
    this.assetRepository = assetRepository;
    this.certificateBindingRepository = certificateBindingRepository;
    this.certificateMapper = certificateMapper;
  }

  @Transactional
  public CertificateBindingResponse create(UUID certificateId, CreateCertificateBindingRequest request) {
    CertificateEntity certificate = certificateRepository.findById(certificateId)
        .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

    AssetEntity asset = assetRepository.findById(request.assetId())
        .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));

    CertificateBindingEntity binding = new CertificateBindingEntity();
    binding.setId(UUID.randomUUID());
    binding.setCertificate(certificate);
    binding.setAsset(asset);
    binding.setBindingType(request.bindingType());
    binding.setEndpoint(request.endpoint());
    binding.setPort(request.port());
    binding.setCreatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));

    CertificateBindingEntity saved = certificateBindingRepository.save(binding);

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<CertificateBindingResponse> listByCertificateId(UUID certificateId) {
    if (!certificateRepository.existsById(certificateId)) {
      throw new ResourceNotFoundException("Certificate not found: " + certificateId);
    }
    return certificateBindingRepository.findByCertificateId(certificateId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CertificateBindingResponse> listByAssetId(UUID assetId) {
    if (!assetRepository.existsById(assetId)) {
      throw new ResourceNotFoundException("Asset not found: " + assetId);
    }
    return certificateBindingRepository.findByAssetId(assetId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CertificateResponse> listCertificatesByAssetId(UUID assetId) {
    if (!assetRepository.existsById(assetId)) {
      throw new ResourceNotFoundException("Asset not found: " + assetId);
    }
    return certificateBindingRepository.findByAssetId(assetId)
        .stream()
        .map(CertificateBindingEntity::getCertificate)
        .map(certificateMapper::toResponse)
        .toList();
  }

  private CertificateBindingResponse toResponse(CertificateBindingEntity binding) {
    return new CertificateBindingResponse(
        binding.getId(),
        binding.getCertificate().getId(),
        binding.getCertificate().getName(),
        binding.getAsset().getId(),
        binding.getAsset().getName(),
        binding.getBindingType(),
        binding.getEndpoint(),
        binding.getPort(),
        binding.getCreatedAt());
  }

}
