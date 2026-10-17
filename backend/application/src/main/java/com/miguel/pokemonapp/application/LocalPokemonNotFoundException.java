package com.miguel.pokemonapp.application;

public class LocalPokemonNotFoundException extends RuntimeException {

  private final int nationalNumber;

  public LocalPokemonNotFoundException(int nationalNumber) {
    super("Pokémon #" + nationalNumber + " is not in the local collection");
    this.nationalNumber = nationalNumber;
  }

  public int nationalNumber() {
    return nationalNumber;
  }
}
