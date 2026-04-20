package com.combotto.controlplane.common;

import org.springframework.stereotype.Component;

@Component
public class TenantAccessValidator {
  public void validateTenantMatch(String requestTenantId, String claimTenantId) {
    if (requestTenantId != null
        && !requestTenantId.isBlank()
        && !requestTenantId.equals(claimTenantId)) {
      throw new BadRequestException("tenantId must match authenticated tenant");
    }
  }
}
