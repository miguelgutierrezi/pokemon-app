package com.miguel.pokemonapp.application.port;

public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String passwordHash);
}
