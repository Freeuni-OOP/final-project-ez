package com.algorythm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.model.Composition;
import com.algorythm.model.Follow;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.security.JwtService;
import com.algorythm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end coverage of the public profile API against a real Postgres,
 * running through the real controller, service, and JSON serialization -
 * nothing mocked. Proves: only the profile owner's published work shows up, a
 * missing user 404s, follow stats/isFollowing reflect the asking user, and the
 * response never leaks the email address. @Transactional rolls each test's
 * writes back.
 */
@AutoConfigureMockMvc
@Transactional
class PublicUserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;
    @Autowired private FollowRepository followRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice-secret@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob-secret@example.com", "hashed-pw"));
    }

    @Test
    void getProfile_returnsOnlyThisUsersPublishedCompositions() throws Exception {
        publish(alice, "Alice public", "aliceslug1");
        compositionRepository.save(new Composition(alice, "Alice private", "pattern", 100));
        publish(bob, "Bob public", "bobslug1");

        mockMvc.perform(get("/api/public/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.compositions.length()").value(1))
                .andExpect(jsonPath("$.compositions[0].slug").value("aliceslug1"));
    }

    @Test
    void getProfile_returnsNotFoundForAnUnknownUsername() throws Exception {
        mockMvc.perform(get("/api/public/users/ghost")).andExpect(status().isNotFound());
    }

    @Test
    void getProfile_followStatsAndIsFollowingReflectTheAskingUser() throws Exception {
        followRepository.save(new Follow(bob.getId(), alice.getId()));

        mockMvc.perform(
                        get("/api/public/users/alice")
                                .header("Authorization", "Bearer " + jwtService.generateToken("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.isFollowing").value(true));

        mockMvc.perform(get("/api/public/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.isFollowing").value(false));
    }

    @Test
    void responseNeverExposesTheEmailAddress() throws Exception {
        publish(alice, "Song", "privacycheck1");

        String body =
                mockMvc.perform(get("/api/public/users/alice"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(body).doesNotContain("alice-secret@example.com").doesNotContain("email");
    }

    private Composition publish(User owner, String title, String slug) {
        Composition composition = new Composition(owner, title, "pattern", 100);
        composition.setSlug(slug);
        composition.setPublic(true);
        return compositionRepository.save(composition);
    }
}