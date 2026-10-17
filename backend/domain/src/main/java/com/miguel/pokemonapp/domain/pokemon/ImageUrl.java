package com.miguel.pokemonapp.domain.pokemon;

public final class ImageUrl {

  private ImageUrl() {}

  public static String resolve(String officialArtworkSprite, String frontDefaultSprite) {
    if (isPresent(officialArtworkSprite)) {
      return officialArtworkSprite;
    }
    if (isPresent(frontDefaultSprite)) {
      return frontDefaultSprite;
    }
    return null;
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isBlank();
  }
}
