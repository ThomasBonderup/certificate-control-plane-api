package com.combotto.controlplane.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestSupport {
  public static final String READ_SCOPE = "controlplane.read";
  public static final String WRITE_SCOPE = "controlplane.write";

  private SecurityTestSupport() {
  }

  public static RequestPostProcessor authenticated() {
    return authenticated("test-user");
  }

  public static RequestPostProcessor authenticated(String subject) {
    return jwt().jwt(token -> token
        .claim("sub", subject)
        .claim("scope", READ_SCOPE + " " + WRITE_SCOPE))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE),
            new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE));
  }

  public static RequestPostProcessor readOnly() {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("scope", READ_SCOPE))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE));
  }

  public static RequestPostProcessor writeOnly() {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("scope", WRITE_SCOPE))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE));
  }
}
