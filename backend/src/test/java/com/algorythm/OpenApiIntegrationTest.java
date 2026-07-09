package com.algorythm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the auto-generated API docs are actually reachable, open without a
 * token (like the rest of the public API), and reflect the real controllers -
 * against a real Postgres (via AbstractIntegrationTest) so the full app
 * context, including SecurityConfig, is exercised exactly as it runs.
 */
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void apiDocs_areReachableWithoutAuthenticationAndListRealEndpoints() throws Exception {
        String body =
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.openapi").exists())
                        .andExpect(jsonPath("$.info.title").value("AlgoRythm API"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // spot-check a few real, currently-existing routes rather than the
        // whole surface, so this doesn't need updating every time an endpoint
        // is added - the whole point is that the docs generate themselves.
        assertThat(body)
                .contains("/api/compositions")
                .contains("/api/auth/register")
                .contains("/api/public/compositions")
                .contains("/api/comments/{id}");
    }

    @Test
    void swaggerUi_isReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}