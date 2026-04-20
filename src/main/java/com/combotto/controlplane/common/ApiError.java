package com.combotto.controlplane.common;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error envelope")
public record ApiError(
  @Schema(description = "Timestamp when the error response was generated.", example = "2026-04-20T12:00:00Z")
  OffsetDateTime timestamp,
  @Schema(description = "HTTP status code.", example = "400")
  int status,
  @Schema(description = "HTTP reason phrase.", example = "Bad Request")
  String error,
  @Schema(description = "Human-readable error message.", example = "tenantId must match authenticated tenant")
  String message,
  @Schema(description = "Request path that triggered the error.", example = "/api/certificates")
  String path
) {}
