package com.miguel.pokemonapp.application;

public class PokemonNotFoundException extends RuntimeException {

  public PokemonNotFoundException(String message) {
    super(message);
  }

  public PokemonNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
