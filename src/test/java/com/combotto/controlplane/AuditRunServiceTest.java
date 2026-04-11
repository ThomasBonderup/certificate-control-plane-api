package com.combotto.controlplane;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.combotto.controlplane.api.CreateAuditRunRequest;
import com.combotto.controlplane.model.AuditRun;
import com.combotto.controlplane.repositories.AuditRunRepository;
import com.combotto.controlplane.services.AuditRunService;

public class AuditRunServiceTest {
  static class FakeRepo implements AuditRunRepository {

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
      return store.values().stream().filter(run -> run.assetId().equals(assetId)).toList();
    }
  }

  @Test
  void create_assignsId_andQueuedStatus() {
    AuditRunService service = new AuditRunService(new FakeRepo());
    CreateAuditRunRequest request = new CreateAuditRunRequest("gw-123", "mqtt-tls");

    AuditRun run = service.create(request);

    assertTrue(run.id() > 0);
    assertEquals("gw-123", run.assetId());
    assertEquals("mqtt-tls", run.profile());
    assertEquals("QUEUED", run.status());
  }
}
