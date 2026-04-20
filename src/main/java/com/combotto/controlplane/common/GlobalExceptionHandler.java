package com.combotto.controlplane.common;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        400,
        "Bad Request",
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        404,
        "Not Found",
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        400,
        "Bad Request",
        "Validation failed",
        request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return new ApiError(
        OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        400,
        "Bad Request",
        "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(),
        request.getRequestURI());
  }
}
