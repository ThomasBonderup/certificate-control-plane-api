package com.combotto.controlplane.errors;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }

}