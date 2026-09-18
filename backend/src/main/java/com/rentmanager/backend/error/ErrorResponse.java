package com.rentmanager.backend.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, List<FieldError> errors) {

  public record FieldError(String field, String code) {}

  public static ErrorResponse of(ErrorCode code) {
    return new ErrorResponse(code.name(), List.of());
  }

  public static ErrorResponse of(ErrorCode code, List<FieldError> errors) {
    return new ErrorResponse(code.name(), errors);
  }
}
