package com.algorythm.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.dto.UserProfileResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
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
 * Unit tests for building a public profile. Repositories and the like/follow
 * services are mocked; the real end-to-end shape (only published work, no email)
 * is covered by PublicUserControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {


    @Mock private UserRepository users;
    @Mock private CompositionRepository compositions;
    @Mock private LikeService likeService;
    @Mock private FollowService followService;


    private UserProfileService userProfileService;


    private final User alice = new User("alice", "alice@example.com", "hashed-pw");


    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(users, compositions, likeService, followService);
    }


    @Test
    void getProfile_returnsThePublishedCompositionsAndStatsForTheUser() {
        Composition published = new Composition(alice, "Song", "pattern", 100);
        PublicCompositionResponse response =
                new PublicCompositionResponse(
                        1L, "slug1", "Song", "pattern", 100, "alice", null, null, 2L, false, List.of());
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(alice))
                .thenReturn(List.of(published));
        when(likeService.toResponses(List.of(published), null)).thenReturn(List.of(response));
        when(followService.followerCount(alice.getId())).thenReturn(5L);
        when(followService.followingCount(alice.getId())).thenReturn(3L);
        when(followService.isFollowing(null, alice.getId())).thenReturn(false);


        UserProfileResponse profile = userProfileService.getProfile("alice", null);


        assertThat(profile.username()).isEqualTo("alice");
        assertThat(profile.followerCount()).isEqualTo(5L);
        assertThat(profile.followingCount()).isEqualTo(3L);
        assertThat(profile.isFollowing()).isFalse();
        assertThat(profile.compositions()).containsExactly(response);
    }


    @Test
    void getProfile_returnsNotFoundForAnUnknownUsername() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());


        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> userProfileService.getProfile("ghost", null));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(compositions, never()).findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(any());
    }


    @Test
    void getProfile_looksUpTheViewerWhenAViewerUsernameIsGiven() {
        User bob = new User("bob", "bob@example.com", "hashed-pw");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(alice))
                .thenReturn(List.of());
        when(likeService.toResponses(List.of(), "bob")).thenReturn(List.of());


        userProfileService.getProfile("alice", "bob");


        // real numeric-id correctness of isFollowing is proven end-to-end by
        // PublicUserControllerIntegrationTest; here we only prove the viewer
        // gets looked up instead of the anonymous (null) path being taken.
        verify(users).findByUsername("bob");
        verify(followService).isFollowing(any(), any());
    }


    @Test
    void getProfile_treatsAnUnresolvableViewerUsernameAsAnonymous() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findByUsername("ghost-viewer")).thenReturn(Optional.empty());
        when(compositions.findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(alice))
                .thenReturn(List.of());
        when(likeService.toResponses(List.of(), "ghost-viewer")).thenReturn(List.of());


        userProfileService.getProfile("alice", "ghost-viewer");


        verify(followService).isFollowing(null, alice.getId());
    }
}



