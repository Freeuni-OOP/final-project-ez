package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algorythm.dto.CompositionRequest;
import com.algorythm.dto.CompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for composition CRUD + publish/unpublish logic. The repository and
 * user repository are mocked so these run without a database; real enforcement
 * of owner-scoping against actual persisted rows is covered separately by
 * CompositionServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class CompositionServiceTest {

    @Mock private CompositionRepository compositions;
    @Mock private UserRepository users;

    private CompositionService compositionService;

    private final User alice = new User("alice", "alice@example.com", "hashed-pw");
    private final User bob = new User("bob", "bob@example.com", "hashed-pw");

    @BeforeEach
    void setUp() {
        compositionService = new CompositionService(compositions, users);
    }

    // --- create ------------------------------------------------------

    @Test
    void create_savesACompositionOwnedByTheCurrentUser() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.save(any(Composition.class))).thenAnswer(inv -> inv.getArgument(0));
        CompositionRequest request = new CompositionRequest("My Song", "C4 E4 G4", 120);

        CompositionResponse response = compositionService.create("alice", request);

        assertThat(response.title()).isEqualTo("My Song");
        assertThat(response.pattern()).isEqualTo("C4 E4 G4");
        assertThat(response.bpm()).isEqualTo(120);
        assertThat(response.isPublic()).isFalse();

        ArgumentCaptor<Composition> captor = ArgumentCaptor.forClass(Composition.class);
        verify(compositions).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(alice);
    }

    @Test
    void create_rejectsWhenTheCurrentUserIsUnknown() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());
        CompositionRequest request = new CompositionRequest("Song", "pattern", 120);

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.create("ghost", request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(compositions, never()).save(any());
    }

    // --- list ----------------------------------------------------------

    @Test
    void list_returnsOnlyTheCurrentUsersCompositions() {
        Composition ownedByAlice = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByOwnerOrderByUpdatedAtDesc(alice))
                .thenReturn(List.of(ownedByAlice));

        List<CompositionResponse> response = compositionService.list("alice");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("Song");
        verify(compositions).findByOwnerOrderByUpdatedAtDesc(alice);
    }

    // --- get -----------------------------------------------------------

    @Test
    void get_returnsACompositionOwnedByTheCurrentUser() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));

        CompositionResponse response = compositionService.get("alice", 5L);

        assertThat(response.title()).isEqualTo("Song");
    }

    @Test
    void get_returnsNotFoundWhenTheCompositionIsMissingOrNotOwned() {
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByIdAndOwner(5L, bob)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> compositionService.get("bob", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- update ----------------------------------------------------------

    @Test
    void update_updatesFieldsOnACompositionOwnedByTheCurrentUser() {
        Composition owned = new Composition(alice, "Old title", "old pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));
        CompositionRequest request = new CompositionRequest("New title", "new pattern", 140);

        CompositionResponse response = compositionService.update("alice", 5L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.pattern()).isEqualTo("new pattern");
        assertThat(response.bpm()).isEqualTo(140);
    }

    @Test
    void update_returnsNotFoundWhenTheCompositionIsNotOwnedByTheCurrentUser() {
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByIdAndOwner(5L, bob)).thenReturn(Optional.empty());
        CompositionRequest request = new CompositionRequest("New title", "new pattern", 140);

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.update("bob", 5L, request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- delete ----------------------------------------------------------

    @Test
    void delete_deletesACompositionOwnedByTheCurrentUser() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));

        compositionService.delete("alice", 5L);

        verify(compositions).delete(owned);
    }

    @Test
    void delete_returnsNotFoundWhenTheCompositionIsNotOwnedByTheCurrentUser() {
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByIdAndOwner(5L, bob)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.delete("bob", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(compositions, never()).delete(any());
    }

    // --- publish / unpublish --------------------------------------------

    @Test
    void publish_assignsAShareSlugAndMarksItPublicOnFirstPublish() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));
        when(compositions.existsBySlug(anyString())).thenReturn(false);

        CompositionResponse response = compositionService.publish("alice", 5L);

        assertThat(response.isPublic()).isTrue();
        assertThat(response.slug()).isNotBlank();
        assertThat(response.slug()).matches("[a-zA-Z0-9]{8}");
    }

    @Test
    void publish_keepsTheExistingSlugOnRepublish() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        owned.setSlug("existing1");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));

        CompositionResponse response = compositionService.publish("alice", 5L);

        assertThat(response.slug()).isEqualTo("existing1");
        assertThat(response.isPublic()).isTrue();
        verify(compositions, never()).existsBySlug(any());
    }

    @Test
    void publish_retriesSlugGenerationOnCollision() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));
        when(compositions.existsBySlug(anyString())).thenReturn(true, false);

        CompositionResponse response = compositionService.publish("alice", 5L);

        assertThat(response.slug()).isNotBlank();
        verify(compositions, org.mockito.Mockito.times(2)).existsBySlug(anyString());
    }

    @Test
    void publish_returnsNotFoundWhenTheCompositionIsNotOwnedByTheCurrentUser() {
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByIdAndOwner(5L, bob)).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.publish("bob", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unpublish_makesItPrivateButKeepsTheShareSlug() {
        Composition owned = new Composition(alice, "Song", "pattern", 100);
        owned.setSlug("keepme12");
        owned.setPublic(true);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(compositions.findByIdAndOwner(5L, alice)).thenReturn(Optional.of(owned));

        CompositionResponse response = compositionService.unpublish("alice", 5L);

        assertThat(response.isPublic()).isFalse();
        assertThat(response.slug()).isEqualTo("keepme12");
    }

    @Test
    void unpublish_returnsNotFoundWhenTheCompositionIsNotOwnedByTheCurrentUser() {
        when(users.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(compositions.findByIdAndOwner(eq(5L), eq(bob))).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.unpublish("bob", 5L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}