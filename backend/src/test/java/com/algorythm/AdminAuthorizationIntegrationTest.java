package com.algorythm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.dto.LoginRequest;
import com.algorythm.dto.RegisterRequest;
import com.algorythm.model.Role;
import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms the central enforcement: /api/admin/** is refused (403) for a normal
 * signed-in user and allowed for an admin, all through the real filter chain +
 * security config — no manual role check in the service.
 */
@AutoConfigureMockMvc
@Transactional
class AdminAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;

    private String registerAndLogin(String username) throws Exception {
        RegisterRequest register =
                new RegisterRequest(username, username + "@example.com", "password123");
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(username, "password123");
        String body =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void normalUserIsForbiddenFromAdminRoutes() throws Exception {
        String token = registerAndLogin("plainuser");
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanReachAdminRoutes() throws Exception {
        String token = registerAndLogin("bossuser");
        User boss = users.findByUsername("bossuser").orElseThrow();
        boss.setRole(Role.ADMIN);
        users.save(boss);

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
