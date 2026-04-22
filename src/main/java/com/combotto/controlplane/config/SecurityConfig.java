package com.combotto.controlplane.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final JsonSecurityErrorWriter jsonSecurityErrorWriter;

  public SecurityConfig(JsonSecurityErrorWriter jsonSecurityErrorWriter) {
    this.jsonSecurityErrorWriter = jsonSecurityErrorWriter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, ex) -> jsonSecurityErrorWriter.write(
                request,
                response,
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource"))
            .accessDeniedHandler((request, response, ex) -> jsonSecurityErrorWriter.write(
                request,
                response,
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Access denied")))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/hello",
                "/healthz",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/metrics", "/actuator/metrics/**")
            .hasAuthority("SCOPE_controlplane.read")
            .requestMatchers(HttpMethod.GET, "/actuator/prometheus")
            .hasAuthority("SCOPE_controlplane.read")
            .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_controlplane.read")
            .requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("SCOPE_controlplane.write")
            .requestMatchers(HttpMethod.PATCH, "/api/**").hasAuthority("SCOPE_controlplane.write")
            .requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("SCOPE_controlplane.write")
            .anyRequest()
            .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);
      Collection<GrantedAuthority> roleAuthorities = extractRoleAuthorities(jwt);

      LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
      if (scopeAuthorities != null) {
        authorities.addAll(scopeAuthorities);
      }
      authorities.addAll(roleAuthorities);
      return authorities;
    });
    return jwtAuthenticationConverter;
  }

  private Collection<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
    LinkedHashSet<String> roles = new LinkedHashSet<>();
    addRoles(roles, jwt.getClaimAsStringList("roles"));

    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null) {
      addRoles(roles, claimAsStringList(realmAccess.get("roles")));
    }

    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null) {
      resourceAccess.values().stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(clientAccess -> claimAsStringList(clientAccess.get("roles")))
          .forEach(clientRoles -> addRoles(roles, clientRoles));
    }

    return roles.stream()
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
  }

  private void addRoles(Collection<String> target, Collection<String> roles) {
    if (roles == null) {
      return;
    }

    roles.stream()
        .map(String::trim)
        .filter(role -> !role.isBlank())
        .forEach(target::add);
  }

  private List<String> claimAsStringList(Object value) {
    if (!(value instanceof Collection<?> values)) {
      return List.of();
    }

    return values.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .toList();
  }
}
