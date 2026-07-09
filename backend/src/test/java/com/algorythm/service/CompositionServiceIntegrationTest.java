package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.algorythm.dto.CompositionRequest;
import com.algorythm.dto.CompositionResponse;
import com.algorythm.model.User;
import com.algorythm.repository.TagRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * End-to-end coverage of CompositionService against a real Postgres (via
 * AbstractIntegrationTest): create/read/update/delete and publish/unpublish run
 * through the real service + repositories, nothing mocked. The focus is proving
 * that every operation stays scoped to the owner - another user's attempt to
 * read, edit, delete, or publish someone else's composition must 404, never
 * leak data or succeed. @Transactional rolls each test's writes back.
 */
@Transactional
class CompositionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CompositionService compositionService;
    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("alice", "alice@example.com", "hashed-pw"));
        userRepository.save(new User("bob", "bob@example.com", "hashed-pw"));
    }

    @Test
    void fullCrudAndPublishFlow_worksForTheOwner() {
        CompositionResponse created =
                compositionService.create(
                        "alice", new CompositionRequest("Song A", "C4 E4 G4", 120, List.of()));
        assertThat(created.id()).isNotNull();
        assertThat(created.isPublic()).isFalse();
        assertThat(created.slug()).isNull();

        CompositionResponse fetched = compositionService.get("alice", created.id());
        assertThat(fetched.title()).isEqualTo("Song A");

        CompositionResponse updated =
                compositionService.update(
                        "alice", created.id(), new CompositionRequest("Song A v2", "C4 E4 G4 C5", 130, List.of()));
        assertThat(updated.title()).isEqualTo("Song A v2");
        assertThat(updated.bpm()).isEqualTo(130);

        CompositionResponse published = compositionService.publish("alice", created.id());
        assertThat(published.isPublic()).isTrue();
        assertThat(published.slug()).isNotBlank();

        CompositionResponse unpublished = compositionService.unpublish("alice", created.id());
        assertThat(unpublished.isPublic()).isFalse();
        assertThat(unpublished.slug()).isEqualTo(published.slug());

        compositionService.delete("alice", created.id());
        assertThrows(
                ResponseStatusException.class, () -> compositionService.get("alice", created.id()));
    }

    @Test
    void publish_reusesTheSameShareSlugAcrossPublishUnpublishCycles() {
        CompositionResponse created =
                compositionService.create("alice", new CompositionRequest("Song", "pattern", 100, List.of()));

        String firstSlug = compositionService.publish("alice", created.id()).slug();
        compositionService.unpublish("alice", created.id());
        String secondSlug = compositionService.publish("alice", created.id()).slug();

        assertThat(secondSlug).isEqualTo(firstSlug);
    }

    @Test
    void list_isScopedToTheCurrentUser() {
        compositionService.create("alice", new CompositionRequest("Alice 1", "pattern", 100, List.of()));
        compositionService.create("alice", new CompositionRequest("Alice 2", "pattern", 100, List.of()));
        compositionService.create("bob", new CompositionRequest("Bob 1", "pattern", 100, List.of()));

        assertThat(compositionService.list("alice")).hasSize(2);
        assertThat(compositionService.list("bob")).hasSize(1);
    }

    @Test
    void create_persistsAndNormalizesTags() {
        CompositionResponse created =
                compositionService.create(
                        "alice", new CompositionRequest("Song", "pattern", 100, List.of("  Lofi  ", "Chill")));

        assertThat(created.tags()).containsExactly("chill", "lofi");
    }

    @Test
    void create_sharesOneTagRowAcrossCompositionsWithTheSameTagName() {
        compositionService.create("alice", new CompositionRequest("Song A", "pattern", 100, List.of("lofi")));
        compositionService.create("bob", new CompositionRequest("Song B", "pattern", 100, List.of("LOFI")));

        assertThat(tagRepository.findAll()).extracting(com.algorythm.model.Tag::getName)
                .containsExactly("lofi");
    }

    @Test
    void update_replacesTheTagSet() {
        CompositionResponse created =
                compositionService.create(
                        "alice", new CompositionRequest("Song", "pattern", 100, List.of("chill")));

        CompositionResponse updated =
                compositionService.update(
                        "alice",
                        created.id(),
                        new CompositionRequest("Song", "pattern", 100, List.of("energetic")));

        assertThat(updated.tags()).containsExactly("energetic");
    }

    @Test
    void create_rejectsMoreThanFiveTags() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.create(
                                "alice",
                                new CompositionRequest(
                                        "Song", "pattern", 100,
                                        List.of("a", "b", "c", "d", "e", "f"))));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anotherUserCannotReadSomeoneElsesComposition() {
        CompositionResponse alicesComposition =
                compositionService.create("alice", new CompositionRequest("Private", "pattern", 100, List.of()));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.get("bob", alicesComposition.id()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anotherUserCannotUpdateSomeoneElsesComposition() {
        CompositionResponse alicesComposition =
                compositionService.create(
                        "alice", new CompositionRequest("Original", "pattern", 100, List.of()));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                compositionService.update(
                                        "bob",
                                        alicesComposition.id(),
                                        new CompositionRequest("Hijacked", "evil pattern", 200, List.of())));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // confirm the failed attempt had no side effect on alice's data
        assertThat(compositionService.get("alice", alicesComposition.id()).title())
                .isEqualTo("Original");
    }

    @Test
    void anotherUserCannotDeleteSomeoneElsesComposition() {
        CompositionResponse alicesComposition =
                compositionService.create("alice", new CompositionRequest("Keep me", "pattern", 100, List.of()));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.delete("bob", alicesComposition.id()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(compositionService.get("alice", alicesComposition.id()).title())
                .isEqualTo("Keep me");
    }

    @Test
    void anotherUserCannotPublishOrUnpublishSomeoneElsesComposition() {
        CompositionResponse alicesComposition =
                compositionService.create("alice", new CompositionRequest("Song", "pattern", 100, List.of()));

        ResponseStatusException publishEx =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.publish("bob", alicesComposition.id()));
        assertThat(publishEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        compositionService.publish("alice", alicesComposition.id());

        ResponseStatusException unpublishEx =
                assertThrows(
                        ResponseStatusException.class,
                        () -> compositionService.unpublish("bob", alicesComposition.id()));
        assertThat(unpublishEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // bob's attempt didn't flip it back to private
        assertThat(compositionService.get("alice", alicesComposition.id()).isPublic()).isTrue();
    }
}