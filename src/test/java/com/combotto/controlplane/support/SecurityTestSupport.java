package com.combotto.controlplane.support;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestSupport {
  public static final String READ_SCOPE = "controlplane.read";
  public static final String WRITE_SCOPE = "controlplane.write";
  public static final String ADMIN_ROLE = "ADMIN";
  public static final String DEMO_TENANT_ID = "demo-tenant";
  public static final String OTHER_TENANT_ID = "other-tenant";

  private SecurityTestSupport() {
  }

  public static RequestPostProcessor authenticated() {
    return authenticated("test-user");
  }

  public static RequestPostProcessor authenticated(String subject) {
    return authenticated(subject, DEMO_TENANT_ID);
  }

  public static RequestPostProcessor authenticated(String subject, String tenantId) {
    return jwt().jwt(token -> token
        .claim("sub", subject)
        .claim("tenantId", tenantId)
        .claim("scope", READ_SCOPE + " " + WRITE_SCOPE))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE),
            new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE));
  }

  public static RequestPostProcessor adminAuthenticated() {
    return adminAuthenticated("admin-user");
  }

  public static RequestPostProcessor adminAuthenticated(String subject) {
    return adminAuthenticated(subject, DEMO_TENANT_ID);
  }

  public static RequestPostProcessor adminAuthenticated(String subject, String tenantId) {
    return jwt().jwt(token -> token
        .claim("sub", subject)
        .claim("tenantId", tenantId)
        .claim("scope", READ_SCOPE + " " + WRITE_SCOPE)
        .claim("roles", List.of(ADMIN_ROLE))
        .claim("realm_access", java.util.Map.of("roles", List.of(ADMIN_ROLE))))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE),
            new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE),
            new SimpleGrantedAuthority("ROLE_" + ADMIN_ROLE));
  }

  public static RequestPostProcessor readOnly() {
    return readOnly(DEMO_TENANT_ID);
  }

  public static RequestPostProcessor readOnly(String tenantId) {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("tenantId", tenantId)
        .claim("scope", READ_SCOPE))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE));
  }

  public static RequestPostProcessor writeOnly() {
    return writeOnly(DEMO_TENANT_ID);
  }

  public static RequestPostProcessor writeOnly(String tenantId) {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("tenantId", tenantId)
        .claim("scope", WRITE_SCOPE))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE));
  }

  public static RequestPostProcessor authenticatedWithoutTenant() {
    return jwt().jwt(token -> token
        .claim("sub", "test-user")
        .claim("scope", READ_SCOPE + " " + WRITE_SCOPE))
        .authorities(
            new SimpleGrantedAuthority("SCOPE_" + READ_SCOPE),
            new SimpleGrantedAuthority("SCOPE_" + WRITE_SCOPE));
  }
}
