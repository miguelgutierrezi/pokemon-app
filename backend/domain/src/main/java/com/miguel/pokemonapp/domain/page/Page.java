package com.miguel.pokemonapp.domain.page;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

  public Page {
    if (content == null) {
      throw new IllegalArgumentException("content must not be null");
    }
    content = List.copyOf(content);
    if (totalElements > 0 && page > totalPages) {
      throw new PageOutOfRangeException("page " + page + " is beyond last page " + totalPages);
    }
  }

  public static <T> Page<T> of(List<T> content, PageRequest request, long totalElements) {
    int totalPages =
        totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
    return new Page<>(content, request.page(), request.size(), totalElements, totalPages);
  }
}
