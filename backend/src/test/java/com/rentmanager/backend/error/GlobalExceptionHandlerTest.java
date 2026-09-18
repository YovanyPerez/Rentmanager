package com.rentmanager.backend.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void apiExceptionKeepsItsCodeAndStatus() {
    ResponseEntity<ErrorResponse> response =
        handler.handleApiException(new ApiException(ErrorCode.PROPERTY_NOT_FOUND));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("PROPERTY_NOT_FOUND");
    assertThat(response.getBody().errors()).isEmpty();
  }

  @Test
  void validationErrorsAreMappedToCodes() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "contract");
    bindingResult.addError(new FieldError("contract", "monthlyRent", null, false,
        new String[] {"Positive"}, null, null));
    MethodParameter parameter =
        new MethodParameter(String.class.getDeclaredMethod("length"), -1);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().errors())
        .containsExactly(new ErrorResponse.FieldError("monthlyRent", "POSITIVE"));
  }

  @Test
  void frameworkExceptionsKeepTheirStatus() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnexpected(new NoResourceFoundException(HttpMethod.GET, "/api/nope", "No static resource"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
  }

  @Test
  void unexpectedExceptionsBecomeInternalError() {
    ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
  }
}
