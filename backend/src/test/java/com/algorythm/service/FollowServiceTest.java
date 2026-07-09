package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algorythm.model.FollowId;
import com.algorythm.model.User;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for follow/unfollow and the counts/isFollowing flag. The
 * repository is mocked; real duplicate prevention against the DB's composite
 * key is covered by FollowServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private FollowRepository follows;
    @Mock private UserRepository users;

    private FollowService followService;

    private final User alice = withId(new User("alice", "alice@example.com", "hashed-pw"), 1L);
    private final User bob = withId(new User("bob", "bob@example.com", "hashed-pw"), 2L);

    @BeforeEach
    void setUp() {
        followService = new FollowService(follows, users);
    }

    // --- follow ------------------------------------------------------------

    @Test
    void follow_savesAFollowRelationship() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(follows.existsById(any(FollowId.class))).thenReturn(false);

        followService.follow("alice", "bob");

        verify(follows).save(any());
    }

    @Test
    void follow_isIdempotent_doesNotSaveWhenAlreadyFollowing() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(follows.existsById(any(FollowId.class))).thenReturn(true);

        followService.follow("alice", "bob");

        verify(follows, never()).save(any());
    }

    @Test
    void follow_rejectsFollowingYourself() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> followService.follow("alice", "alice"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(follows, never()).save(any());
    }

    @Test
    void follow_rejectsAnUnknownTargetUser() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> followService.follow("alice", "ghost"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void follow_rejectsAnUnauthenticatedFollower() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> followService.follow("ghost", "bob"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(follows, never()).save(any());
    }

    // --- unfollow ----------------------------------------------------------

    @Test
    void unfollow_removesTheFollowRelationship() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));

        followService.unfollow("alice", "bob");

        verify(follows).deleteByIdFollowerIdAndIdFollowingId(1L, 2L);
    }

    @Test
    void unfollow_isHarmlessWhenNotFollowing() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));

        // the repository's delete-by-query is a no-op when no row matches;
        // the service must not throw either way.
        followService.unfollow("alice", "bob");

        verify(follows).deleteByIdFollowerIdAndIdFollowingId(1L, 2L);
    }

    @Test
    void unfollow_rejectsAnUnknownTargetUser() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> followService.unfollow("alice", "ghost"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- counts / isFollowing ------------------------------------------------

    @Test
    void followerCount_delegatesToTheRepository() {
        when(follows.countByIdFollowingId(1L)).thenReturn(7L);

        assertThat(followService.followerCount(1L)).isEqualTo(7L);
    }

    @Test
    void followingCount_delegatesToTheRepository() {
        when(follows.countByIdFollowerId(1L)).thenReturn(4L);

        assertThat(followService.followingCount(1L)).isEqualTo(4L);
    }

    @Test
    void isFollowing_isFalseWithoutHittingTheRepositoryWhenViewerIsAnonymous() {
        assertThat(followService.isFollowing(null, 2L)).isFalse();
        verify(follows, never()).existsByIdFollowerIdAndIdFollowingId(any(), any());
    }

    @Test
    void isFollowing_reflectsTheRepositoryWhenViewerIsKnown() {
        when(follows.existsByIdFollowerIdAndIdFollowingId(1L, 2L)).thenReturn(true);

        assertThat(followService.isFollowing(1L, 2L)).isTrue();
    }

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