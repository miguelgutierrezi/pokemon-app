package com.miguel.pokemonapp.web.auth.validation;

import com.miguel.pokemonapp.domain.user.Email;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return Email.isValid(value);
  }
}
