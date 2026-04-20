package com.combotto.controlplane.common;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-Correlation-Id";
  private static final String MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = Optional.ofNullable(request.getHeader(HEADER_NAME))
        .filter(value -> !value.isBlank())
        .orElse(UUID.randomUUID().toString());

    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER_NAME, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

}
