package com.miguel.pokemonapp.web.error;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.DuplicateEmailException;
import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.InvalidCredentialsException;
import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.application.StaleLocalPokemonException;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import jakarta.validation.ConstraintViolation;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final URI BAD_REQUEST_TYPE = URI.create("urn:problem-type:bad-request");
  private static final URI NOT_FOUND_TYPE = URI.create("urn:problem-type:not-found");
  private static final URI CONFLICT_TYPE = URI.create("urn:problem-type:conflict");
  private static final URI STALE_VERSION_TYPE = URI.create("urn:problem-type:stale-version");
  private static final URI UNSUPPORTED_MEDIA_TYPE =
      URI.create("urn:problem-type:unsupported-media-type");
  private static final URI BAD_GATEWAY_TYPE = URI.create("urn:problem-type:bad-gateway");
  private static final URI INTERNAL_ERROR_TYPE = URI.create("urn:problem-type:internal");

  /** Shared with the security entry point for missing/invalid Bearer tokens. */
  public static final URI UNAUTHORIZED_TYPE = URI.create("urn:problem-type:unauthorized");

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
    return badRequest(ex, headers, request, parameter);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String parameter = ex.getPropertyName();
    if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
      parameter = mismatch.getName();
    }
    if (parameter == null || parameter.isBlank()) {
      parameter = "request";
    }
    return badRequest(ex, headers, request, parameter);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String parameter = firstValidationField(ex).orElse("request");
    return badRequest(ex, headers, request, parameter);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return handleExceptionInternal(
        ex,
        problem(HttpStatus.BAD_REQUEST, BAD_REQUEST_TYPE, unreadableBodyDetail(ex)),
        headers,
        HttpStatus.BAD_REQUEST,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return handleExceptionInternal(
        ex,
        problem(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
        headers,
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        request);
  }

  @ExceptionHandler(PageOutOfRangeException.class)
  ProblemDetail handlePageOutOfRange(PageOutOfRangeException ex) {
    return problem(HttpStatus.BAD_REQUEST, BAD_REQUEST_TYPE, ex.getMessage());
  }

  @ExceptionHandler(PokemonNotFoundException.class)
  ProblemDetail handlePokemonNotFound(PokemonNotFoundException ex) {
    return problem(
        HttpStatus.NOT_FOUND, NOT_FOUND_TYPE, "Pokémon " + ex.nationalId() + " was not found");
  }

  @ExceptionHandler(LocalPokemonNotFoundException.class)
  ProblemDetail handleLocalPokemonNotFound(LocalPokemonNotFoundException ex) {
    return problem(
        HttpStatus.NOT_FOUND,
        NOT_FOUND_TYPE,
        "Pokémon " + ex.nationalNumber() + " is not in the local collection");
  }

  @ExceptionHandler(DuplicateLocalPokemonException.class)
  ProblemDetail handleDuplicateLocalPokemon(DuplicateLocalPokemonException ex) {
    return problem(
        HttpStatus.CONFLICT,
        CONFLICT_TYPE,
        "Pokémon " + ex.nationalNumber() + " is already in the local collection");
  }

  @ExceptionHandler(StaleLocalPokemonException.class)
  ProblemDetail handleStaleLocalPokemon(StaleLocalPokemonException ex) {
    return problem(
        HttpStatus.CONFLICT,
        STALE_VERSION_TYPE,
        "Pokémon " + ex.nationalNumber() + " was modified by another request; reload and retry");
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
    return problem(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_TYPE, InvalidCredentialsException.DETAIL);
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
    return problem(HttpStatus.CONFLICT, CONFLICT_TYPE, "An account with this email already exists");
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

  private ResponseEntity<Object> badRequest(
      Exception ex, HttpHeaders headers, WebRequest request, String parameter) {
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

  private static ProblemDetail problem(HttpStatus status, URI type, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    return problem;
  }

  private static String unreadableBodyDetail(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    if (cause instanceof UnrecognizedPropertyException unrecognized) {
      return "Unknown or read-only property '" + unrecognized.getPropertyName() + "'";
    }
    if (cause instanceof MismatchedInputException mismatched) {
      String propertyName = propertyNameFromPath(mismatched);
      if (propertyName != null) {
        return "Invalid or missing value for property '" + propertyName + "'";
      }
    }
    return "Malformed request body";
  }

  private static String propertyNameFromPath(MismatchedInputException ex) {
    var path = ex.getPath();
    if (path.isEmpty()) {
      return null;
    }
    String name = path.get(path.size() - 1).getPropertyName();
    if (name == null || name.isBlank()) {
      return null;
    }
    return name;
  }

  private static Optional<String> firstValidationField(HandlerMethodValidationException ex) {
    for (ParameterValidationResult result : ex.getParameterValidationResults()) {
      if (result instanceof ParameterErrors errors && errors.hasFieldErrors()) {
        FieldError fieldError = errors.getFieldError();
        if (fieldError != null) {
          return Optional.of(fieldError.getField());
        }
      }
      for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
        ConstraintViolation<?> violation = result.unwrap(resolvable, ConstraintViolation.class);
        if (violation != null) {
          String path = simplifyPropertyPath(violation.getPropertyPath().toString());
          if (!path.isBlank()) {
            return Optional.of(path);
          }
        }
      }
      String parameterName = result.getMethodParameter().getParameterName();
      if (parameterName != null && !parameterName.isBlank()) {
        return Optional.of(parameterName);
      }
    }
    return Optional.empty();
  }

  private static String simplifyPropertyPath(String path) {
    Objects.requireNonNull(path, "path");
    String suffix = ".<list element>";
    if (path.endsWith(suffix)) {
      return path.substring(0, path.length() - suffix.length());
    }
    return path;
  }
}
