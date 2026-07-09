package com.algorythm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.dto.LoginRequest;
import com.algorythm.dto.RegisterRequest;
import com.algorythm.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end auth coverage against a real Postgres (via AbstractIntegrationTest):
 * register/login run through the real AuthController + AuthService + BCrypt, and
 * /api/auth/me runs through the real JwtAuthenticationFilter + SecurityConfig
 * filter chain — nothing mocked. @Transactional rolls each test's DB writes back
 * so tests don't leak users into each other.
 */
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registerThenLoginThenMe_roundTripsThroughTheRealFilterChain() throws Exception {
        RegisterRequest register = new RegisterRequest("alice", "alice@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));

        LoginRequest login = new LoginRequest("alice", "password123");
        String loginBody =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tokenType").value("Bearer"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = objectMapper.readTree(loginBody).get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_rejectsADuplicateUsername() throws Exception {
        RegisterRequest first = new RegisterRequest("bob", "bob@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest("bob", "bob2@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_rejectsADuplicateEmail() throws Exception {
        RegisterRequest first = new RegisterRequest("carol", "carol@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest("carol2", "carol@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_rejectsWrongPasswordForARealPersistedUser() throws Exception {
        RegisterRequest register = new RegisterRequest("dave", "dave@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest("dave", "wrong-password");
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_rejectsAnUnknownIdentifier() throws Exception {
        LoginRequest login = new LoginRequest("ghost", "password123");
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // SecurityConfig doesn't configure httpBasic()/formLogin(), so Spring
    // Security's default AuthenticationEntryPoint rejects an unauthenticated
    // request to a protected route with 403, not 401 — verified against the
    // real filter chain rather than assumed.

    @Test
    void me_rejectsAMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());
    }

    @Test
    void me_rejectsAMalformedToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_rejectsATokenSignedWithAnUnknownKey() throws Exception {
        // Same shape as a real token but signed with a key the running app never
        // used, so JwtService.validateAndGetUsername must reject it on signature
        // verification, not just parse it and trust it.
        String bogusToken =
                Jwts.builder()
                        .subject("alice")
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 60_000))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "a-completely-different-signing-key-0123456789ab"
                                                .getBytes()))
                        .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + bogusToken))
                .andExpect(status().isForbidden());
    }
}