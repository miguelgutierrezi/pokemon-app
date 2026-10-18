package com.miguel.pokemonapp.web.auth.validation;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

  private int maxBytes;

  @Override
  public void initialize(MaxUtf8Bytes annotation) {
    maxBytes = annotation.value();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.getBytes(UTF_8).length <= maxBytes;
  }
}
