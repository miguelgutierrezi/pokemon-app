package com.miguel.pokemonapp.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.application.pokemon.DeleteLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.GetLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.GetPokemonDetailUseCase;
import com.miguel.pokemonapp.application.pokemon.ImportPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.UpdateLocalPokemonUseCase;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.web.collection.CollectionController;
import com.miguel.pokemonapp.web.pokemon.PokemonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = {PokemonController.class, CollectionController.class})
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

  private static final String CATALOG_NOT_FOUND_DETAIL = "Pokémon 25 was not found";
  private static final String LOCAL_NOT_FOUND_DETAIL = "Pokémon 25 is not in the local collection";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListPokemonUseCase listPokemonUseCase;

  @MockitoBean private GetPokemonDetailUseCase getPokemonDetailUseCase;

  @MockitoBean private ImportPokemonUseCase importPokemonUseCase;

  @MockitoBean private ListLocalPokemonUseCase listLocalPokemonUseCase;

  @MockitoBean private GetLocalPokemonUseCase getLocalPokemonUseCase;

  @MockitoBean private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

  @MockitoBean private DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;

  @Test
  void beanValidationPageZeroReturnsProblemDetailNamingPage() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon").param("page", "0")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("page");
    assertNoLeak(body);
  }

  @Test
  void beanValidationSizeAboveMaxReturnsProblemDetailNamingSize() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon").param("size", "21")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("size");
    assertNoLeak(body);
  }

  @Test
  void typeMismatchPageReturnsBadRequestProblem() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon").param("page", "abc")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertNoLeak(body);
  }

  @Test
  void pokemonNotFoundReturnsNotFoundProblemWithoutLeakingUpstream() throws Exception {
    when(getPokemonDetailUseCase.execute(9999))
        .thenThrow(new PokemonNotFoundException(9999, "catalog returned 404 for /pokemon/{id}"));

    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon/9999")), 404)
            .andExpect(jsonPath("$.detail").value("Pokémon 9999 was not found"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("catalog returned").doesNotContain("/pokemon/{id}");
    assertNoLeak(body);
  }

  @Test
  void typeMismatchDetailIdReturnsBadRequestProblemNamingId() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon/abc")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("id");
    assertThat(body).doesNotContain("catalog returned").doesNotContain("/pokemon/{id}");
    assertNoLeak(body);
  }

  @Test
  void detailIdZeroReturnsBadRequestProblemNamingId() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon/0")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("id");
    assertThat(body).doesNotContain("catalog returned").doesNotContain("/pokemon/{id}");
    assertNoLeak(body);
  }

  @Test
  void pageOutOfRangeReturnsBadRequestProblem() throws Exception {
    when(listPokemonUseCase.execute(any()))
        .thenThrow(new PageOutOfRangeException("page 115 is beyond last page 114"));

    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon").param("page", "115")), 400)
            .andExpect(jsonPath("$.detail").value("page 115 is beyond last page 114"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertNoLeak(body);
  }

  @Test
  void catalogUnavailableReturnsBadGatewayWithoutLeakingUpstream() throws Exception {
    when(listPokemonUseCase.execute(any()))
        .thenThrow(
            new CatalogUnavailableException(
                "catalog unavailable for https://pokeapi.co/api/v2/pokemon-species"));

    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon")), 502)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("pokeapi.co").doesNotContain("pokemon-species");
    assertNoLeak(body);
  }

  @Test
  void unexpectedRuntimeExceptionReturnsInternalServerErrorProblem() throws Exception {
    when(listPokemonUseCase.execute(any()))
        .thenThrow(new RuntimeException("secret boom at https://internal.example/x"));

    String body =
        expectProblem(mockMvc.perform(get("/api/v1/pokemon")), 500)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .doesNotContain("secret boom")
        .doesNotContain("internal.example")
        .doesNotContain("RuntimeException");
    assertNoLeak(body);
  }

  @Test
  void duplicateLocalPokemonReturnsConflictProblem() throws Exception {
    when(importPokemonUseCase.execute(25)).thenThrow(new DuplicateLocalPokemonException(25));

    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalNumber\":25}")),
                409)
            .andExpect(jsonPath("$.type").value("urn:problem-type:conflict"))
            .andExpect(jsonPath("$.detail").value("Pokémon 25 is already in the local collection"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertNoLeak(body);
  }

  @Test
  void localPokemonNotFoundReturnsNotFoundProblemDistinctFromCatalog() throws Exception {
    when(getLocalPokemonUseCase.execute(25)).thenThrow(new LocalPokemonNotFoundException(25));

    String body =
        expectProblem(mockMvc.perform(get("/api/v1/collection/25")), 404)
            .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
            .andExpect(jsonPath("$.detail").value(LOCAL_NOT_FOUND_DETAIL))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(LOCAL_NOT_FOUND_DETAIL).isNotEqualTo(CATALOG_NOT_FOUND_DETAIL);
    assertThat(body).doesNotContain("was not found");
    assertNoLeak(body);
  }

  @Test
  void missingNationalNumberReturnsBadRequestNamingField() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("nationalNumber");
    assertNoLeak(body);
  }

  @Test
  void zeroNationalNumberReturnsBadRequestNamingField() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalNumber\":0}")),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("nationalNumber");
    assertNoLeak(body);
  }

  @Test
  void nonNumericNationalNumberReturnsMalformedBodyProblem() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalNumber\":\"abc\"}")),
                400)
            .andExpect(jsonPath("$.type").value("urn:problem-type:bad-request"))
            .andExpect(jsonPath("$.detail").value("Malformed request body"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .doesNotContain("ImportPokemonRequest")
        .doesNotContain("JsonMappingException")
        .doesNotContain("MismatchedInputException")
        .doesNotContain("com.fasterxml")
        .doesNotContain("tools.jackson");
    assertNoLeak(body);
  }

  @Test
  void malformedJsonReturnsMalformedBodyProblem() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{")),
                400)
            .andExpect(jsonPath("$.type").value("urn:problem-type:bad-request"))
            .andExpect(jsonPath("$.detail").value("Malformed request body"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .doesNotContain("JsonParseException")
        .doesNotContain("Unexpected end-of-input")
        .doesNotContain("com.fasterxml")
        .doesNotContain("tools.jackson");
    assertNoLeak(body);
  }

  @Test
  void collectionNationalNumberZeroReturnsBadRequestNamingField() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/collection/0")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("nationalNumber");
    assertNoLeak(body);
  }

  @Test
  void collectionNationalNumberNonNumericReturnsBadRequestNamingField() throws Exception {
    String body =
        expectProblem(mockMvc.perform(get("/api/v1/collection/abc")), 400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("nationalNumber");
    assertNoLeak(body);
  }

  @Test
  void importCatalogNotFoundReturnsNotFoundProblem() throws Exception {
    when(importPokemonUseCase.execute(anyInt()))
        .thenThrow(new PokemonNotFoundException(9999, "catalog returned 404 for /pokemon/{id}"));

    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalNumber\":9999}")),
                404)
            .andExpect(jsonPath("$.detail").value("Pokémon 9999 was not found"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("catalog returned").doesNotContain("/pokemon/{id}");
    assertNoLeak(body);
  }

  @Test
  void importCatalogUnavailableReturnsBadGateway() throws Exception {
    when(importPokemonUseCase.execute(anyInt()))
        .thenThrow(
            new CatalogUnavailableException(
                "catalog unavailable for https://pokeapi.co/api/v2/pokemon/1"));

    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalNumber\":1}")),
                502)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("pokeapi.co").doesNotContain("/pokemon/1");
    assertNoLeak(body);
  }

  private static ResultActions expectProblem(ResultActions actions, int status) throws Exception {
    return actions
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").exists())
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  private static void assertNoLeak(String body) {
    assertThat(body)
        .doesNotContain("PageOutOfRangeException")
        .doesNotContain("CatalogUnavailableException")
        .doesNotContain("PokemonNotFoundException")
        .doesNotContain("DuplicateLocalPokemonException")
        .doesNotContain("LocalPokemonNotFoundException")
        .doesNotContain("MethodArgumentNotValidException")
        .doesNotContain("HandlerMethodValidationException")
        .doesNotContain("MethodArgumentTypeMismatchException")
        .doesNotContain("HttpMessageNotReadableException")
        .doesNotContain("\tat ")
        .doesNotContain("stackTrace")
        .doesNotContain("https://");
  }
}
