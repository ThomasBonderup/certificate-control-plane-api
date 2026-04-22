package com.combotto.controlplane.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.combotto.controlplane.common.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonSecurityErrorWriter {
  private final ObjectMapper objectMapper;

  public JsonSecurityErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiError body = new ApiError(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI());

    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
