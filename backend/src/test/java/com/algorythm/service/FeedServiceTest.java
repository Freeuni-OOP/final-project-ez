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
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the following feed. Repositories and LikeService are mocked;
 * real scoping/ordering against real rows is covered by
 * FeedServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private FollowRepository follows;
    @Mock private CompositionRepository compositions;
    @Mock private UserRepository users;
    @Mock private LikeService likeService;

    private FeedService feedService;

    private final User alice = new User("alice", "alice@example.com", "hashed-pw");

    @BeforeEach
    void setUp() {
        feedService = new FeedService(follows, compositions, users, likeService);
    }

    @Test
    void following_returnsEmptyWithoutQueryingCompositionsWhenFollowingNobody() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(follows.findFollowingIds(alice.getId())).thenReturn(List.of());

        List<PublicCompositionResponse> result = feedService.following("alice", 0, 20);

        assertThat(result).isEmpty();
        verify(compositions, never()).findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), any());
    }

    @Test
    void following_usesTheRequestedPageAndSize() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(follows.findFollowingIds(alice.getId())).thenReturn(List.of(2L, 3L));
        when(compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());

        feedService.following("alice", 1, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions)
                .findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void following_clampsAnOversizedPageSizeDownToTheMax() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(follows.findFollowingIds(alice.getId())).thenReturn(List.of(2L));
        when(compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());

        feedService.following("alice", 0, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions)
                .findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void following_queriesOnlyTheFollowedOwnerIds() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(follows.findFollowingIds(alice.getId())).thenReturn(List.of(2L, 3L));
        when(compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());

        feedService.following("alice", 0, 20);

        verify(compositions).findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(
                eq(List.of(2L, 3L)), any());
    }

    @Test
    void following_returnsWhatLikeServiceBuildsForTheAskingViewer() {
        Composition composition = new Composition(alice, "Song", "pattern", 100);
        PublicCompositionResponse response =
                new PublicCompositionResponse(
                        "slug1", "Song", "pattern", 100, "alice", null, null, 0L, false);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(follows.findFollowingIds(alice.getId())).thenReturn(List.of(2L));
        when(compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of(composition));
        when(likeService.toResponses(List.of(composition), "alice")).thenReturn(List.of(response));

        List<PublicCompositionResponse> result = feedService.following("alice", 0, 20);

        assertThat(result).containsExactly(response);
    }

    @Test
    void following_rejectsAnUnauthenticatedUser() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> feedService.following("ghost", 0, 20));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}