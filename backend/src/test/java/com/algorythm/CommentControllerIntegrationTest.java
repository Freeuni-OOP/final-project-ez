package com.algorythm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.dto.CommentRequest;
import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CommentRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.security.JwtService;
import com.algorythm.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end coverage of comments against a real Postgres, running through the
 * real controllers, service, and JSON serialization - nothing mocked. Proves:
 * comments list oldest-first for a public composition, posting requires auth,
 * only the author may delete their own comment, and the response never leaks
 * the author's email. @Transactional rolls each test's writes back.
 */
@AutoConfigureMockMvc
@Transactional
class CommentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;
    @Autowired private CommentRepository commentRepository;

    private User alice;
    private User bob;
    private Composition publicComposition;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice-priv@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob-priv@example.com", "hashed-pw"));

        Composition composition = new Composition(alice, "Song", "pattern", 100);
        composition.setPublic(true);
        composition.setSlug("commentable1");
        publicComposition = compositionRepository.save(composition);
    }

    // --- reading -------------------------------------------------------------

    @Test
    void listComments_returnsThemInPostedOrder() throws Exception {
        commentRepository.save(new Comment(publicComposition, alice, "First"));
        commentRepository.save(new Comment(publicComposition, bob, "Second"));

        mockMvc.perform(get("/api/public/compositions/commentable1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("First"))
                .andExpect(jsonPath("$[0].author").value("alice"))
                .andExpect(jsonPath("$[1].body").value("Second"))
                .andExpect(jsonPath("$[1].author").value("bob"));
    }

    @Test
    void listComments_returnsNotFoundForAPrivateComposition() throws Exception {
        // mirrors CompositionService#unpublish: slug kept, isPublic flipped false
        Composition privateComposition = new Composition(alice, "Draft", "pattern", 100);
        privateComposition.setSlug("privatecomments1");
        privateComposition.setPublic(false);
        compositionRepository.save(privateComposition);

        mockMvc.perform(get("/api/public/compositions/privatecomments1/comments"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listComments_returnsNotFoundForAnUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/public/compositions/no-such-slug/comments"))
                .andExpect(status().isNotFound());
    }

    // --- posting ---------------------------------------------------------

    @Test
    void postComment_addsACommentForTheLoggedInUser() throws Exception {
        mockMvc.perform(
                        post("/api/compositions/" + publicComposition.getId() + "/comments")
                                .header("Authorization", "Bearer " + jwtService.generateToken("bob"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CommentRequest("Great song!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("bob"))
                .andExpect(jsonPath("$.body").value("Great song!"));

        mockMvc.perform(get("/api/public/compositions/commentable1/comments"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void postComment_requiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/compositions/" + publicComposition.getId() + "/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CommentRequest("Hi"))))
                .andExpect(status().isForbidden());
    }

    // --- deleting --------------------------------------------------------

    @Test
    void deleteComment_theAuthorCanDeleteTheirOwnComment() throws Exception {
        Comment comment = commentRepository.save(new Comment(publicComposition, bob, "Delete me"));

        mockMvc.perform(
                        delete("/api/comments/" + comment.getId())
                                .header("Authorization", "Bearer " + jwtService.generateToken("bob")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/public/compositions/commentable1/comments"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteComment_refusesAnyoneOtherThanTheAuthor() throws Exception {
        Comment comment = commentRepository.save(new Comment(publicComposition, bob, "Keep me"));

        mockMvc.perform(
                        delete("/api/comments/" + comment.getId())
                                .header("Authorization", "Bearer " + jwtService.generateToken("alice")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/public/compositions/commentable1/comments"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Keep me"));
    }

    @Test
    void deleteComment_returnsNotFoundForAnUnknownComment() throws Exception {
        mockMvc.perform(
                        delete("/api/comments/999999")
                                .header("Authorization", "Bearer " + jwtService.generateToken("bob")))
                .andExpect(status().isNotFound());
    }

    // --- privacy -----------------------------------------------------------

    @Test
    void commentResponsesNeverExposeTheAuthorsEmailAddress() throws Exception {
        commentRepository.save(new Comment(publicComposition, alice, "Hello"));

        String body =
                mockMvc.perform(get("/api/public/compositions/commentable1/comments"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(body).doesNotContain("alice-priv@example.com").doesNotContain("email");
    }
}