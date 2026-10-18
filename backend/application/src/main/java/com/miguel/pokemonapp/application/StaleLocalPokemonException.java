package com.miguel.pokemonapp.application;

public class StaleLocalPokemonException extends RuntimeException {

  private final int nationalNumber;

  public StaleLocalPokemonException(int nationalNumber) {
    super("Pokémon #" + nationalNumber + " was modified by another request; reload and retry");
    this.nationalNumber = nationalNumber;
  }

  public int nationalNumber() {
    return nationalNumber;
  }
}
