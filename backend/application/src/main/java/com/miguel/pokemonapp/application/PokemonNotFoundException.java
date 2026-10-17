package com.miguel.pokemonapp.application;

public class PokemonNotFoundException extends RuntimeException {

  private final int nationalId;

  public PokemonNotFoundException(int nationalId, String message) {
    super(message);
    this.nationalId = nationalId;
  }

  public PokemonNotFoundException(int nationalId, String message, Throwable cause) {
    super(message, cause);
    this.nationalId = nationalId;
  }

  public int nationalId() {
    return nationalId;
  }
}
