package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.CompositionLikeId;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionLikeRepository;
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
 * Unit tests for like/unlike and for enriching public composition responses
 * with like count + likedByMe. The repositories are mocked; real duplicate
 * prevention against the DB's composite key is covered by
 * LikeServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private CompositionLikeRepository likes;
    @Mock private CompositionRepository compositions;
    @Mock private UserRepository users;

    private LikeService likeService;

    private final User alice = new User("alice", "alice@example.com", "hashed-pw");

    @BeforeEach
    void setUp() {
        likeService = new LikeService(likes, compositions, users);
    }

    // --- like ------------------------------------------------------------

    @Test
    void like_savesALikeForAPublicComposition() {
        Composition composition = publicComposition();
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(5L)).thenReturn(Optional.of(composition));
        when(likes.existsById(any(CompositionLikeId.class))).thenReturn(false);

        likeService.like("alice", 5L);

        verify(likes).save(any());
    }

    @Test
    void like_doesNotSaveADuplicateWhenAlreadyLiked() {
        Composition composition = publicComposition();
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(5L)).thenReturn(Optional.of(composition));
        when(likes.existsById(any(CompositionLikeId.class))).thenReturn(true);

        likeService.like("alice", 5L);

        verify(likes, never()).save(any());
    }

    @Test
    void like_rejectsAPrivateComposition() {
        Composition privateComposition = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(5L)).thenReturn(Optional.of(privateComposition));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> likeService.like("alice", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(likes, never()).save(any());
    }

    @Test
    void like_rejectsAnUnknownComposition() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> likeService.like("alice", 999L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void like_rejectsAnUnauthenticatedUser() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> likeService.like("ghost", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(compositions, never()).findById(any());
    }

    // --- unlike ----------------------------------------------------------

    @Test
    void unlike_removesTheLike() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        likeService.unlike("alice", 5L);

        verify(likes).deleteByIdUserIdAndIdCompositionId(alice.getId(), 5L);
    }

    @Test
    void unlike_isHarmlessWhenNeverLiked() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        // the repository's delete-by-query is a no-op when no row matches;
        // the service must not throw either way.
        likeService.unlike("alice", 5L);

        verify(likes).deleteByIdUserIdAndIdCompositionId(alice.getId(), 5L);
    }

    @Test
    void unlike_rejectsAnUnauthenticatedUser() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> likeService.unlike("ghost", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(likes, never()).deleteByIdUserIdAndIdCompositionId(any(), any());
    }

    // --- toResponse / toResponses ------------------------------------------

    @Test
    void toResponse_reflectsTheCountAndWhetherTheViewerLikedIt() {
        Composition composition = publicComposition();
        User bob = withId(new User("bob", "b@x.com", "pw"), 42L);
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(likes.countByIdCompositionId(any())).thenReturn(3L);
        when(likes.existsByIdUserIdAndIdCompositionId(eq(42L), any())).thenReturn(true);

        PublicCompositionResponse response = likeService.toResponse(composition, "bob");

        assertThat(response.likeCount()).isEqualTo(3L);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    void toResponse_likedByMeIsFalseWhenNobodyIsLoggedIn() {
        Composition composition = publicComposition();
        when(likes.countByIdCompositionId(any())).thenReturn(3L);

        PublicCompositionResponse response = likeService.toResponse(composition, null);

        assertThat(response.likedByMe()).isFalse();
        verify(likes, never()).existsByIdUserIdAndIdCompositionId(any(), any());
        verify(users, never()).findByUsername(any());
    }

    @Test
    void toResponses_isEmptyWhenGivenNoCompositions() {
        List<PublicCompositionResponse> responses = likeService.toResponses(List.of(), "alice");

        assertThat(responses).isEmpty();
        verify(likes, never()).countByCompositionIdIn(any());
        verify(likes, never()).likedCompositionIds(any(), any());
    }

    private Composition publicComposition() {
        Composition composition = new Composition(alice, "Song", "pattern", 100);
        composition.setPublic(true);
        return composition;
    }

    /**
     * User's id is only ever assigned by JPA on insert; this test needs a real,
     * distinguishable id (never null) to exercise the likedByMe short-circuit
     * without spinning up a database.
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