package com.combotto.controlplane.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestSupport {

  private SecurityTestSupport() {
  }

  public static RequestPostProcessor authenticated() {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("scope", "api.read api.write"))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_api.read"),
            new SimpleGrantedAuthority("SCOPE_api.write"));
  }
}
