package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AuditRun;

public class AuditRunMapper {
  private AuditRunMapper() {
  }

  public static AuditRunResponse toResponse(AuditRun run) {
    return new AuditRunResponse(
        run.id(),
        run.assetId(),
        run.profile(),
        run.status());
  }

}
