package com.miguel.pokemonapp.web;

import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
class OpenApiDocsIntegrationTest {

  @Test
  void apiDocsReturnsOpenApiJson(@Autowired RestTestClient restClient) {
    restClient
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.openapi")
        .exists()
        .jsonPath("$.info.title")
        .isEqualTo("Pokéfolio API")
        .jsonPath("$.paths['/api/v1/pokemon'].get.parameters[?(@.name=='page')].in")
        .isEqualTo("query")
        .jsonPath("$.paths['/api/v1/pokemon'].get.parameters[?(@.name=='page')].required")
        .isEqualTo(false)
        .jsonPath("$.paths['/api/v1/pokemon'].get.parameters[?(@.name=='size')].in")
        .isEqualTo("query")
        .jsonPath("$.paths['/api/v1/pokemon'].get.parameters[?(@.name=='size')].required")
        .isEqualTo(false)
        .jsonPath("$.paths['/api/v1/pokemon'].get.responses['200']")
        .exists()
        .jsonPath("$.paths['/api/v1/pokemon'].get.responses['400']")
        .exists()
        .jsonPath("$.paths['/api/v1/pokemon'].get.responses['502']")
        .exists();
  }
}
