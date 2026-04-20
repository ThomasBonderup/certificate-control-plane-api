package com.combotto.controlplane.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/hello",
                "/healthz",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info")
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
            .authenticated()
            )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
