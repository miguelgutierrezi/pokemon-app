package com.miguel.pokemonapp.web.error;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final URI BAD_REQUEST_TYPE = URI.create("urn:problem-type:bad-request");
  private static final URI BAD_GATEWAY_TYPE = URI.create("urn:problem-type:bad-gateway");
  private static final URI INTERNAL_ERROR_TYPE = URI.create("urn:problem-type:internal");

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String parameter =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getField)
            .findFirst()
            .orElse("request");
    return handleExceptionInternal(
        ex,
        problem(
            HttpStatus.BAD_REQUEST,
            BAD_REQUEST_TYPE,
            "Invalid value for parameter '" + parameter + "'"),
        headers,
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(PageOutOfRangeException.class)
  ProblemDetail handlePageOutOfRange(PageOutOfRangeException ex) {
    return problem(HttpStatus.BAD_REQUEST, BAD_REQUEST_TYPE, ex.getMessage());
  }

  @ExceptionHandler(CatalogUnavailableException.class)
  ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
    logger.error("Pokémon catalog unavailable", ex);
    return problem(
        HttpStatus.BAD_GATEWAY, BAD_GATEWAY_TYPE, "The Pokémon catalog is temporarily unavailable");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    logger.error("Unexpected error", ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_TYPE, "An unexpected error occurred");
  }

  private static ProblemDetail problem(HttpStatus status, URI type, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    return problem;
  }
}
