package com.miguel.pokemonapp.application;

public class InvalidCredentialsException extends RuntimeException {

  public static final String DETAIL = "Invalid email or password";

  public InvalidCredentialsException() {
    super(DETAIL);
  }
}
