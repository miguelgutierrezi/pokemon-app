package com.miguel.pokemonapp.web.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.InvalidCredentialsException;
import com.miguel.pokemonapp.application.auth.AuthSession;
import com.miguel.pokemonapp.application.auth.AuthenticateUserUseCase;
import com.miguel.pokemonapp.application.auth.RegisterUserUseCase;
import com.miguel.pokemonapp.web.error.GlobalExceptionHandler;
import com.miguel.pokemonapp.web.support.ImportSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityTestConfig
class AuthControllerTest {

  private static final String TOKEN = "header.payload.sig";
  private static final String VALID_PASSWORD = "Pikachu12";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RegisterUserUseCase registerUserUseCase;

  @MockitoBean private AuthenticateUserUseCase authenticateUserUseCase;

  @Test
  void registerValidReturnsCreatedSessionShapeWithoutSecrets() throws Exception {
    when(registerUserUseCase.execute("Ash", "ash@example.com", VALID_PASSWORD))
        .thenReturn(session(7L, "Ash", "ash@example.com"));

    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Ash",
                          "email": "ash@example.com",
                          "password": "%s"
                        }
                        """
                            .formatted(VALID_PASSWORD)))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.accessToken").value(TOKEN))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600))
            .andExpect(jsonPath("$.user.id").value(7))
            .andExpect(jsonPath("$.user.name").value("Ash"))
            .andExpect(jsonPath("$.user.email").value("ash@example.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .doesNotContain("password")
        .doesNotContain("passwordHash")
        .doesNotContain(VALID_PASSWORD);
    verify(registerUserUseCase).execute("Ash", "ash@example.com", VALID_PASSWORD);
  }

  @Test
  void registerMixedCaseEmailAcceptedByEdgeWithNormalisedResponse() throws Exception {
    when(registerUserUseCase.execute(eq("Ash"), eq("Ash@Pokefolio.dev"), eq(VALID_PASSWORD)))
        .thenReturn(session(1L, "Ash", "ash@pokefolio.dev"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ash", "Ash@Pokefolio.dev", VALID_PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("ash@pokefolio.dev"));

    verify(registerUserUseCase).execute("Ash", "Ash@Pokefolio.dev", VALID_PASSWORD);
  }

  @Test
  void registerEmailWithSurroundingSpacesAcceptedByEdge() throws Exception {
    when(registerUserUseCase.execute(eq("Ash"), eq(" ash2@pokefolio.dev "), eq(VALID_PASSWORD)))
        .thenReturn(session(2L, "Ash", "ash2@pokefolio.dev"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ash", " ash2@pokefolio.dev ", VALID_PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("ash2@pokefolio.dev"));

    verify(registerUserUseCase).execute("Ash", " ash2@pokefolio.dev ", VALID_PASSWORD);
  }

  @Test
  void registerPasswordOverSeventyTwoUtf8BytesReturnsBadRequestNamingPassword() throws Exception {
    String oversize = "ñ".repeat(40);

    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ash", "ash@example.com", oversize))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("password");
    verify(registerUserUseCase, never()).execute(anyString(), anyString(), anyString());
  }

  @Test
  void registerShortPasswordReturnsBadRequestNamingPassword() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ash", "ash@example.com", "short"))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("password");
    verify(registerUserUseCase, never()).execute(anyString(), anyString(), anyString());
  }

  @Test
  void registerPasswordOfEightSpacesReturnsBadRequestNamingPassword() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ash", "ash@example.com", "        "))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("password");
    verify(registerUserUseCase, never()).execute(anyString(), anyString(), anyString());
  }

  @Test
  void registerBlankNameReturnsBadRequestNamingName() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("  ", "ash@example.com", VALID_PASSWORD))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("name");
  }

  @Test
  void registerNameOverMaxLengthReturnsBadRequestNamingName() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("n".repeat(101), "ash@example.com", VALID_PASSWORD))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("name");
  }

  @Test
  void registerInvalidEmailReturnsBadRequestNamingEmail() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ash", "not-an-email", VALID_PASSWORD))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("email");
  }

  @Test
  void registerMissingPasswordReturnsBadRequestNamingPassword() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "name": "Ash",
                              "email": "ash@example.com"
                            }
                            """)),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("password");
  }

  @Test
  void registerUnknownPropertyReturnsBadRequestNamingIt() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "name": "Ash",
                              "email": "ash@example.com",
                              "password": "%s",
                              "role": "admin"
                            }
                            """
                                .formatted(VALID_PASSWORD))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("role");
  }

  @Test
  void loginValidReturnsOkSessionShapeWithCacheControl() throws Exception {
    when(authenticateUserUseCase.execute("ash@example.com", VALID_PASSWORD))
        .thenReturn(session(7L, "Ash", "ash@example.com"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ash@example.com", VALID_PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.accessToken").value(TOKEN))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.user.id").value(7))
        .andExpect(jsonPath("$.user.name").value("Ash"))
        .andExpect(jsonPath("$.user.email").value("ash@example.com"));

    verify(authenticateUserUseCase).execute("ash@example.com", VALID_PASSWORD);
  }

  @Test
  void loginShortPasswordReachesUseCase() throws Exception {
    when(authenticateUserUseCase.execute("ash@example.com", "abc"))
        .thenReturn(session(7L, "Ash", "ash@example.com"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("ash@example.com", "abc")))
        .andExpect(status().isOk());

    verify(authenticateUserUseCase).execute("ash@example.com", "abc");
  }

  @Test
  void loginBlankEmailReturnsBadRequest() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("  ", VALID_PASSWORD))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("email");
    verify(authenticateUserUseCase, never()).execute(anyString(), anyString());
  }

  @Test
  void loginBlankPasswordReturnsBadRequest() throws Exception {
    String body =
        expectProblem(
                mockMvc.perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ash@example.com", "  "))),
                400)
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).containsIgnoringCase("password");
    verify(authenticateUserUseCase, never()).execute(anyString(), anyString());
  }

  @Test
  void loginInvalidCredentialsReturnsUnauthorizedProblem() throws Exception {
    when(authenticateUserUseCase.execute("ash@example.com", "wrong-pass"))
        .thenThrow(new InvalidCredentialsException());

    expectProblem(
            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ash@example.com", "wrong-pass"))),
            401)
        .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
        .andExpect(jsonPath("$.detail").value("Invalid email or password"))
        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  void registerNonJsonContentTypeReturnsUnsupportedMediaType() throws Exception {
    expectProblem(
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content(registerJson("Ash", "ash@example.com", VALID_PASSWORD))),
        415);
  }

  @Test
  void loginNonJsonContentTypeReturnsUnsupportedMediaType() throws Exception {
    expectProblem(
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content(loginJson("ash@example.com", VALID_PASSWORD))),
        415);
  }

  private static AuthSession session(long id, String name, String email) {
    return new AuthSession(TOKEN, AuthSession.BEARER, 3600L, new AuthSession.User(id, name, email));
  }

  private static String registerJson(String name, String email, String password) {
    return """
        {
          "name": %s,
          "email": %s,
          "password": %s
        }
        """
        .formatted(jsonString(name), jsonString(email), jsonString(password));
  }

  private static String loginJson(String email, String password) {
    return """
        {
          "email": %s,
          "password": %s
        }
        """
        .formatted(jsonString(email), jsonString(password));
  }

  private static String jsonString(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
}
