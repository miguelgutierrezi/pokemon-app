package com.miguel.pokemonapp.application.port;

public interface AccessTokenIssuer {

  IssuedAccessToken issue(long userId);
}
