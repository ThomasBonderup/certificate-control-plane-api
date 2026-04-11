package com.combotto.controlplane.services;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.combotto.controlplane.api.CreateAuditRunRequest;
import com.combotto.controlplane.model.AuditRun;
import com.combotto.controlplane.repositories.AuditRunRepository;
import com.combotto.controlplane.errors.NotFoundException;

@Service
public class AuditRunService {

  private final AuditRunRepository repo;
  private final AtomicLong ids = new AtomicLong(0);

  public AuditRunService(AuditRunRepository repo) {
    this.repo = repo;
  }

  public AuditRun create(CreateAuditRunRequest req) {
    long id = ids.incrementAndGet();
    AuditRun run = new AuditRun(id, req.assetId(), req.Profile(), "QUEUED");
    return repo.save(run);
  }

  public AuditRun getById(long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("AuditRun" + id + "not found"));
  }

  public List<AuditRun> getByAssetId(String assetId) {
    return repo.findByAssetId(assetId);
  }
}