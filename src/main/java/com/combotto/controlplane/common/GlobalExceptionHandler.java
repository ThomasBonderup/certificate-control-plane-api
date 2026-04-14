package com.combotto.controlplane.common;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now(),
        404,
        "Not Found",
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now(),
        400,
        "Bad Request",
        "Validation failed",
        request.getRequestURI());
  }
}
