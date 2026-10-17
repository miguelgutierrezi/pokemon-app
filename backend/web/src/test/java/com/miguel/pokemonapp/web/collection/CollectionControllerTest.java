package com.miguel.pokemonapp.web.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.pokemon.GetLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ImportPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListLocalPokemonUseCase;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

  private static final Instant SYNCED_AT = Instant.parse("2026-01-01T00:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ImportPokemonUseCase importPokemonUseCase;

  @MockitoBean private ListLocalPokemonUseCase listLocalPokemonUseCase;

  @MockitoBean private GetLocalPokemonUseCase getLocalPokemonUseCase;

  @Test
  void importReturnsCreatedWithLocationAndBodyShape() throws Exception {
    when(importPokemonUseCase.execute(1)).thenReturn(bulbasaur());

    String body =
        mockMvc
            .perform(
                post("/api/v1/collection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nationalNumber\":1}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/collection/1"))
            .andExpect(jsonPath("$.nationalNumber").value(1))
            .andExpect(jsonPath("$.name").value("Bulbasaur"))
            .andExpect(jsonPath("$.category").value("Seed Pokémon"))
            .andExpect(jsonPath("$.types[0]").value("grass"))
            .andExpect(jsonPath("$.types[1]").value("poison"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/1.png"))
            .andExpect(jsonPath("$.generation").value("generation-i"))
            .andExpect(jsonPath("$.syncedAt").value("2026-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.localizedName").value(nullValue()))
            .andExpect(jsonPath("$.location").value(nullValue()))
            .andExpect(jsonPath("$.tags").isArray())
            .andExpect(jsonPath("$.tags[0]").value("forest"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).contains("\"heightM\":0.7");
    assertThat(body).contains("\"weightKg\":6.9");
  }

  @Test
  void listReturnsPaginationEnvelopeWithDefaults() throws Exception {
    when(listLocalPokemonUseCase.execute(any()))
        .thenReturn(
            Page.of(List.of(bulbasaur()), new PageRequest(1, PageRequest.DEFAULT_SIZE), 1L));

    mockMvc
        .perform(get("/api/v1/collection"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nationalNumber").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Bulbasaur"))
        .andExpect(jsonPath("$.content[0].tags[0]").value("forest"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(PageRequest.DEFAULT_SIZE))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    verify(listLocalPokemonUseCase).execute(captor.capture());
    assertThat(captor.getValue().page()).isEqualTo(1);
    assertThat(captor.getValue().size()).isEqualTo(PageRequest.DEFAULT_SIZE);
  }

  @Test
  void getByNationalNumberReturnsOk() throws Exception {
    when(getLocalPokemonUseCase.execute(1)).thenReturn(bulbasaur());

    mockMvc
        .perform(get("/api/v1/collection/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nationalNumber").value(1))
        .andExpect(jsonPath("$.name").value("Bulbasaur"));
  }

  @Test
  void rejectsMissingNationalNumber() throws Exception {
    mockMvc
        .perform(post("/api/v1/collection").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsZeroNationalNumber() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/collection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nationalNumber\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsNonNumericNationalNumber() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/collection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nationalNumber\":\"abc\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(post("/api/v1/collection").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest());
  }

  private static LocalPokemon bulbasaur() {
    return new LocalPokemon(
        1,
        "Bulbasaur",
        "Seed Pokémon",
        List.of("grass", "poison"),
        new BigDecimal("0.7"),
        new BigDecimal("6.9"),
        "https://example.com/1.png",
        "generation-i",
        SYNCED_AT,
        null,
        null,
        Tags.of(List.of("forest")));
  }
}
