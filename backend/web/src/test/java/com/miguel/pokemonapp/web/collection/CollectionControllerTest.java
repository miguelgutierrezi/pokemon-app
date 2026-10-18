package com.miguel.pokemonapp.web.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.pokemon.DeleteLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.GetLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ImportPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ProprietaryUpdate;
import com.miguel.pokemonapp.application.pokemon.UpdateLocalPokemonUseCase;
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
  private static final String VALID_PUT_BODY =
      """
      {
        "localizedName": "Bulbizarre",
        "location": "Pallet Town garden",
        "tags": ["first-partner", "forest"],
        "version": 0
      }
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ImportPokemonUseCase importPokemonUseCase;

  @MockitoBean private ListLocalPokemonUseCase listLocalPokemonUseCase;

  @MockitoBean private GetLocalPokemonUseCase getLocalPokemonUseCase;

  @MockitoBean private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

  @MockitoBean private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

  @Test
  void importReturnsCreatedWithLocationAndBodyShape() throws Exception {
    when(importPokemonUseCase.execute(1)).thenReturn(bulbasaur(0L));

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
            .andExpect(jsonPath("$.version").value(0))
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
            Page.of(List.of(bulbasaur(0L)), new PageRequest(1, PageRequest.DEFAULT_SIZE), 1L));

    mockMvc
        .perform(get("/api/v1/collection"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nationalNumber").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Bulbasaur"))
        .andExpect(jsonPath("$.content[0].tags[0]").value("forest"))
        .andExpect(jsonPath("$.content[0].version").value(0))
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
    when(getLocalPokemonUseCase.execute(1)).thenReturn(bulbasaur(0L));

    mockMvc
        .perform(get("/api/v1/collection/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nationalNumber").value(1))
        .andExpect(jsonPath("$.name").value("Bulbasaur"))
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  void updateReturnsOkWithVersionFromUseCase() throws Exception {
    when(updateLocalPokemonUseCase.execute(eq(1), any(ProprietaryUpdate.class)))
        .thenReturn(
            new LocalPokemon(
                1,
                "Bulbasaur",
                "Seed Pokémon",
                List.of("grass", "poison"),
                new BigDecimal("0.7"),
                new BigDecimal("6.9"),
                "https://example.com/1.png",
                "generation-i",
                SYNCED_AT,
                "Bulbizarre",
                "Pallet Town garden",
                Tags.of(List.of("first-partner", "forest")),
                1L));

    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PUT_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nationalNumber").value(1))
        .andExpect(jsonPath("$.name").value("Bulbasaur"))
        .andExpect(jsonPath("$.localizedName").value("Bulbizarre"))
        .andExpect(jsonPath("$.location").value("Pallet Town garden"))
        .andExpect(jsonPath("$.tags[0]").value("first-partner"))
        .andExpect(jsonPath("$.tags[1]").value("forest"))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.syncedAt").value("2026-01-01T00:00:00Z"));

    ArgumentCaptor<ProprietaryUpdate> captor = ArgumentCaptor.forClass(ProprietaryUpdate.class);
    verify(updateLocalPokemonUseCase).execute(eq(1), captor.capture());
    assertThat(captor.getValue().localizedName()).isEqualTo("Bulbizarre");
    assertThat(captor.getValue().location()).isEqualTo("Pallet Town garden");
    assertThat(captor.getValue().tags()).containsExactly("first-partner", "forest");
    assertThat(captor.getValue().expectedVersion()).isEqualTo(0L);
  }

  @Test
  void updateAcceptsExplicitNullLocalizedNameAndPassesNull() throws Exception {
    when(updateLocalPokemonUseCase.execute(eq(1), any(ProprietaryUpdate.class)))
        .thenReturn(bulbasaur(1L));

    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "localizedName": null,
                      "location": "somewhere",
                      "tags": [],
                      "version": 0
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));

    ArgumentCaptor<ProprietaryUpdate> captor = ArgumentCaptor.forClass(ProprietaryUpdate.class);
    verify(updateLocalPokemonUseCase).execute(eq(1), captor.capture());
    assertThat(captor.getValue().localizedName()).isNull();
    assertThat(captor.getValue().location()).isEqualTo("somewhere");
    assertThat(captor.getValue().tags()).isEmpty();
    assertThat(captor.getValue().expectedVersion()).isEqualTo(0L);
  }

  @Test
  void updateRejectsOmittedLocalizedName() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "location": "somewhere",
                      "tags": [],
                      "version": 0
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRejectsNullTags() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "localizedName": "x",
                      "location": "y",
                      "tags": null,
                      "version": 0
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRejectsBlankTag() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "localizedName": "x",
                      "location": "y",
                      "tags": ["  "],
                      "version": 0
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRejectsUnknownProperty() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "localizedName": "x",
                      "location": "y",
                      "tags": [],
                      "version": 0,
                      "extra": true
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRejectsReadOnlySnapshotProperty() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "localizedName": "x",
                      "location": "y",
                      "tags": [],
                      "version": 0,
                      "name": "Bulbasaur"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRejectsNonJsonContentType() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/1").contentType(MediaType.TEXT_PLAIN).content(VALID_PUT_BODY))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void updateRejectsInvalidPathNationalNumber() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/collection/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PUT_BODY))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/collection/1")).andExpect(status().isNoContent());

    verify(deleteLocalPokemonUseCase).execute(1);
  }

  @Test
  void importRejectsUnknownProperty() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/collection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nationalNumber\":1,\"extra\":true}"))
        .andExpect(status().isBadRequest());
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

  private static LocalPokemon bulbasaur(long version) {
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
        Tags.of(List.of("forest")),
        version);
  }
}
