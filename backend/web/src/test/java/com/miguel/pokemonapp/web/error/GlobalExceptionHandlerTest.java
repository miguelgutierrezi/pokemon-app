package com.miguel.pokemonapp.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.web.pokemon.PokemonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PokemonController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListPokemonUseCase listPokemonUseCase;

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
        .doesNotContain("MethodArgumentNotValidException")
        .doesNotContain("\tat ")
        .doesNotContain("stackTrace");
  }
}
