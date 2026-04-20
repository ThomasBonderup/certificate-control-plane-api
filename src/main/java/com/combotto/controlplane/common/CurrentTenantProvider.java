package com.combotto.controlplane.common;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantProvider {

  public String getRequiredTenantId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new InsufficientAuthenticationException("Missing authenticated JWT");
    }

    String tenantId = jwt.getClaimAsString("tenantId");
    if (tenantId == null || tenantId.isBlank()) {
      throw new InsufficientAuthenticationException("Missing tenantId claim");
    }
    return tenantId;
  }
}
