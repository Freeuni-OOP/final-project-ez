package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algorythm.dto.CommentRequest;
import com.algorythm.dto.CommentResponse;
import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CommentRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for comment reads/posting/deletion. The repositories are mocked;
 * real ordering, HTTP status shapes, and the no-email guarantee are covered by
 * CommentControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository comments;
    @Mock private CompositionRepository compositions;
    @Mock private UserRepository users;

    private CommentService commentService;

    private final User alice = new User("alice", "alice@example.com", "hashed-pw");

    @BeforeEach
    void setUp() {
        commentService = new CommentService(comments, compositions, users);
    }

    // --- listForPublicComposition -------------------------------------------

    @Test
    void listForPublicComposition_returnsCommentsOldestFirst() {
        Composition composition = publicComposition();
        Comment first = new Comment(composition, alice, "First");
        Comment second = new Comment(composition, alice, "Second");
        when(compositions.findBySlugAndIsPublicTrue("share1"))
                .thenReturn(Optional.of(composition));
        when(comments.findByCompositionOrderByCreatedAtAsc(composition))
                .thenReturn(List.of(first, second));

        List<CommentResponse> result = commentService.listForPublicComposition("share1");

        assertThat(result).extracting(CommentResponse::body).containsExactly("First", "Second");
    }

    @Test
    void listForPublicComposition_returnsNotFoundForAnUnknownOrPrivateSlug() {
        when(compositions.findBySlugAndIsPublicTrue("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> commentService.listForPublicComposition("missing"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- add -------------------------------------------------------------

    @Test
    void add_savesACommentAuthoredByTheCurrentUser() {
        Composition composition = publicComposition();
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(5L)).thenReturn(Optional.of(composition));
        when(comments.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.add("alice", 5L, new CommentRequest("Nice one!"));

        assertThat(response.body()).isEqualTo("Nice one!");
        assertThat(response.author()).isEqualTo("alice");
    }

    @Test
    void add_rejectsAPrivateComposition() {
        Composition privateComposition = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(5L)).thenReturn(Optional.of(privateComposition));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> commentService.add("alice", 5L, new CommentRequest("Hi")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(comments, never()).save(any());
    }

    @Test
    void add_rejectsAnUnknownComposition() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> commentService.add("alice", 999L, new CommentRequest("Hi")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void add_rejectsAnUnauthenticatedUser() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> commentService.add("ghost", 5L, new CommentRequest("Hi")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(compositions, never()).findById(any());
    }

    // --- delete ------------------------------------------------------------

    @Test
    void delete_removesTheCommentWhenCalledByItsAuthor() {
        User author = withId(new User("alice", "alice@example.com", "hashed-pw"), 1L);
        Comment comment = new Comment(publicComposition(), author, "Hi");
        when(users.findByUsername("alice")).thenReturn(Optional.of(author));
        when(comments.findById(9L)).thenReturn(Optional.of(comment));

        commentService.delete("alice", 9L);

        verify(comments).delete(comment);
    }

    @Test
    void delete_refusesADifferentUser() {
        User author = withId(new User("alice", "alice@example.com", "hashed-pw"), 1L);
        User someoneElse = withId(new User("bob", "bob@example.com", "hashed-pw"), 2L);
        Comment comment = new Comment(publicComposition(), author, "Hi");
        when(users.findByUsername("bob")).thenReturn(Optional.of(someoneElse));
        when(comments.findById(9L)).thenReturn(Optional.of(comment));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> commentService.delete("bob", 9L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(comments, never()).delete(any());
    }

    @Test
    void delete_returnsNotFoundForAnUnknownComment() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(comments.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> commentService.delete("alice", 999L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_rejectsAnUnauthenticatedUser() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> commentService.delete("ghost", 9L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(comments, never()).findById(any());
    }

    private Composition publicComposition() {
        Composition composition = new Composition(alice, "Song", "pattern", 100);
        composition.setPublic(true);
        return composition;
    }

    /**
     * A Comment's author id is only ever assigned by JPA on insert; the
     * author-only-delete check compares real ids, so these tests need a
     * non-null, distinguishable id without spinning up a database.
     */
    private static User withId(User user, long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
