package com.miguel.pokemonapp.application;

public class DuplicateEmailException extends RuntimeException {

  private final String email;

  public DuplicateEmailException(String email) {
    super("Email is already registered");
    this.email = email;
  }

  public String email() {
    return email;
  }
}
