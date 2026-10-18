package com.miguel.pokemonapp.application.port;

public record IssuedAccessToken(String accessToken, long expiresInSeconds) {}
