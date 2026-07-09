package com.algorythm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.model.Composition;
import com.algorythm.model.CompositionLike;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionLikeRepository;
import com.algorythm.repository.CompositionRepository;
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
 * End-to-end coverage of the public composition read API (explore feed + share
 * links) against a real Postgres, running through the real controller, service,
 * and JSON serialization - nothing mocked. Proves: only published work is
 * visible, ordering/paging behave, a private or missing item 404s, and the
 * response never leaks the owner's email. @Transactional rolls each test's
 * writes back.
 */
@AutoConfigureMockMvc
@Transactional
class PublicCompositionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;
    @Autowired private CompositionLikeRepository likeRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice-secret@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob-secret@example.com", "hashed-pw"));
    }

    // --- feed --------------------------------------------------------------

    @Test
    void feed_returnsOnlyPublishedCompositionsNewestEditFirst() throws Exception {
        Composition older = publish(alice, "Older", "slugold1");
        Composition newer = publish(alice, "Newer", "slugnew1");
        compositionRepository.save(new Composition(alice, "Private draft", "pattern", 100));

        // touch "older" last so it's the most recently updated
        older.setTitle("Older, edited");
        compositionRepository.saveAndFlush(older);

        mockMvc.perform(get("/api/public/compositions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("slugold1"))
                .andExpect(jsonPath("$[1].slug").value("slugnew1"));
    }

    @Test
    void feed_respectsPageAndSizeParameters() throws Exception {
        Composition first = publish(alice, "First", "slugp1");
        Composition second = publish(alice, "Second", "slugp2");
        Composition third = publish(alice, "Third", "slugp3");
        // give each a distinct, increasing updatedAt: third > second > first
        second.setTitle("Second, edited");
        compositionRepository.saveAndFlush(second);
        third.setTitle("Third, edited");
        compositionRepository.saveAndFlush(third);

        mockMvc.perform(get("/api/public/compositions").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("slugp3"))
                .andExpect(jsonPath("$[1].slug").value("slugp2"));

        mockMvc.perform(get("/api/public/compositions").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("slugp1"));
    }

    @Test
    void feed_doesNotErrorOnAnOversizedSizeParameter() throws Exception {
        publish(alice, "Song", "slugbig1");

        mockMvc.perform(get("/api/public/compositions").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // --- getBySlug -----------------------------------------------------------

    @Test
    void getBySlug_returnsThePublishedCompositionWithLikeInfoForTheAskingUser() throws Exception {
        Composition composition = publish(alice, "Song", "share001");
        likeRepository.save(new CompositionLike(bob.getId(), composition.getId()));

        mockMvc.perform(
                        get("/api/public/compositions/share001")
                                .header("Authorization", "Bearer " + jwtService.generateToken("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        mockMvc.perform(
                        get("/api/public/compositions/share001")
                                .header(
                                        "Authorization",
                                        "Bearer " + jwtService.generateToken("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));

        mockMvc.perform(get("/api/public/compositions/share001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    @Test
    void getBySlug_returnsNotFoundForAPrivateComposition() throws Exception {
        // mirrors CompositionService#unpublish: slug kept, isPublic flipped false
        Composition composition = new Composition(alice, "Unpublished", "pattern", 100);
        composition.setSlug("unpub001");
        composition.setPublic(false);
        compositionRepository.save(composition);

        mockMvc.perform(get("/api/public/compositions/unpub001")).andExpect(status().isNotFound());
    }

    @Test
    void getBySlug_returnsNotFoundForAnUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/public/compositions/no-such-slug"))
                .andExpect(status().isNotFound());
    }

    // --- privacy -------------------------------------------------------------

    @Test
    void responsesNeverExposeTheOwnersEmailAddress() throws Exception {
        publish(alice, "Song", "privacycheck1");

        String feedBody =
                mockMvc.perform(get("/api/public/compositions"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(feedBody).doesNotContain("alice-secret@example.com").doesNotContain("email");

        String slugBody =
                mockMvc.perform(get("/api/public/compositions/privacycheck1"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(slugBody).doesNotContain("alice-secret@example.com").doesNotContain("email");
    }

    private Composition publish(User owner, String title, String slug) {
        Composition composition = new Composition(owner, title, "pattern", 100);
        composition.setSlug(slug);
        composition.setPublic(true);
        return compositionRepository.save(composition);
    }
}