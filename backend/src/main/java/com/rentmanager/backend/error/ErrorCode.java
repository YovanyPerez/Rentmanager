package com.rentmanager.backend.error;

import org.springframework.http.HttpStatus;

/**
 * Language-neutral error codes returned by the API. The frontend translates them
 * (for example {@code errors.PROPERTY_NOT_FOUND}).
 */
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
  BAD_REQUEST(HttpStatus.BAD_REQUEST),
  INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND),
  OWNER_NOT_FOUND(HttpStatus.NOT_FOUND),
  TENANT_NOT_FOUND(HttpStatus.NOT_FOUND),
  PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND),
  CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND),
  PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
  MAINTENANCE_NOT_FOUND(HttpStatus.NOT_FOUND),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
  FORBIDDEN(HttpStatus.FORBIDDEN),
  EMAIL_ALREADY_USED(HttpStatus.CONFLICT),
  USER_ROLE_MISMATCH(HttpStatus.BAD_REQUEST),
  USER_ALREADY_LINKED(HttpStatus.CONFLICT),
  OWNER_HAS_PROPERTIES(HttpStatus.CONFLICT),
  TENANT_HAS_CONTRACTS(HttpStatus.CONFLICT),
  PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT),
  CONFLICT(HttpStatus.CONFLICT),
  CONFLICT_ACTIVE_CONTRACT(HttpStatus.CONFLICT),
  INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
