package com.rentmanager.backend.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    log.warn("API error: {}", exception.getCode());
    return ResponseEntity.status(exception.getCode().status())
        .body(ErrorResponse.of(exception.getCode()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    List<ErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), validationCode(fieldError)))
        .toList();
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, errors));
  }

  /**
   * Framework exceptions that already carry an HTTP status implement
   * {@link org.springframework.web.ErrorResponse} (unknown path, wrong method, ...).
   * Keep that status instead of returning 500.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    if (exception instanceof org.springframework.web.ErrorResponse errorResponse) {
      HttpStatusCode status = errorResponse.getStatusCode();
      log.warn("Request error {}: {}", status.value(), exception.getMessage());
      return ResponseEntity.status(status).body(ErrorResponse.of(codeFor(status)));
    }
    log.error("Unexpected error", exception);
    return ResponseEntity.internalServerError().body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }

  /** Bad requests that do not implement {@link org.springframework.web.ErrorResponse}. */
  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
    log.warn("Bad request: {}", exception.getMessage());
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.BAD_REQUEST));
  }

  /** Database constraint violations that were not pre-checked (unique keys, foreign keys, ...). */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
    log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ErrorCode.CONFLICT));
  }

  private static ErrorCode codeFor(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> ErrorCode.BAD_REQUEST;
      case 404 -> ErrorCode.NOT_FOUND;
      case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
      default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.BAD_REQUEST;
    };
  }

  private static String validationCode(FieldError fieldError) {
    String constraint = fieldError.getCode();
    return constraint == null ? "INVALID" : validationCode(constraint);
  }

  private static String validationCode(String constraint) {
    return switch (constraint) {
      case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
      case "Email" -> "EMAIL";
      case "Positive" -> "POSITIVE";
      case "PositiveOrZero" -> "POSITIVE_OR_ZERO";
      case "Size" -> "SIZE";
      case "Pattern" -> "PATTERN";
      case "Min", "DecimalMin" -> "MIN";
      case "Max", "DecimalMax" -> "MAX";
      case "Future", "FutureOrPresent" -> "FUTURE";
      case "Past", "PastOrPresent" -> "PAST";
      default -> "INVALID";
    };
  }
}
