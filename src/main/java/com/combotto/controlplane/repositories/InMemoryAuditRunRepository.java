package com.combotto.controlplane.repositories;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import com.combotto.controlplane.model.AuditRun;

@Repository
public class InMemoryAuditRunRepository implements AuditRunRepository {

  private final Map<Long, AuditRun> store = new ConcurrentHashMap<>();

  @Override
  public AuditRun save(AuditRun run) {
    store.put(run.id(), run);
    return run;
  }

  @Override
  public Optional<AuditRun> findById(long id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<AuditRun> findByAssetId(String assetId) {
    return store.values().stream()
        .filter(run -> run.assetId().equals(assetId))
        .toList();
  }
}
