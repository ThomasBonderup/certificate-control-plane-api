package com.combotto.controlplane.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
  public String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || authentication.getPrincipal() == null) {
      return "system";
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof Jwt jwt) {
      return jwt.getSubject();
    }

    return authentication.getName();
  }
}
