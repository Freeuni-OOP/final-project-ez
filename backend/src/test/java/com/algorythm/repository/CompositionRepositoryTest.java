package com.algorythm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for the composition queries against a real Postgres (via
 * AbstractIntegrationTest). The private, owner-scoped queries and the public
 * explore/profile/feed queries are exercised directly here so their filtering
 * and ordering are proven against real rows, not mocks.
 * @Transactional rolls each test's writes back.
 */
@Transactional
class CompositionRepositoryTest extends AbstractIntegrationTest {

    @Autowired private CompositionRepository compositions;
    @Autowired private UserRepository users;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = users.save(new User("alice", "alice@example.com", "hashed-pw"));
        bob = users.save(new User("bob", "bob@example.com", "hashed-pw"));
    }

    // --- findByOwnerOrderByUpdatedAtDesc ---------------------------------

    @Test
    void findByOwnerOrderByUpdatedAtDesc_returnsOnlyThatOwnersCompositionsNewestEditFirst() {
        Composition older = compositions.save(new Composition(alice, "Older", "pattern", 100));
        Composition newer = compositions.save(new Composition(alice, "Newer", "pattern", 100));
        compositions.save(new Composition(bob, "Not alice's", "pattern", 100));

        // touch "older" last so its updatedAt is guaranteed to be the most recent
        older.setTitle("Older, edited");
        compositions.saveAndFlush(older);

        List<Composition> result = compositions.findByOwnerOrderByUpdatedAtDesc(alice);

        assertThat(result).extracting(Composition::getId).containsExactly(older.getId(), newer.getId());
    }

    // --- findByIdAndOwner --------------------------------------------------

    @Test
    void findByIdAndOwner_findsACompositionForItsOwner() {
        Composition owned = compositions.save(new Composition(alice, "Song", "pattern", 100));

        assertThat(compositions.findByIdAndOwner(owned.getId(), alice)).isPresent();
    }

    @Test
    void findByIdAndOwner_missesForAnyOtherUser() {
        Composition ownedByAlice = compositions.save(new Composition(alice, "Song", "pattern", 100));

        assertThat(compositions.findByIdAndOwner(ownedByAlice.getId(), bob)).isEmpty();
    }

    @Test
    void findByIdAndOwner_missesForAnUnknownId() {
        assertThat(compositions.findByIdAndOwner(999_999L, alice)).isEmpty();
    }

    // --- existsBySlug ------------------------------------------------------

    @Test
    void existsBySlug_reflectsWhetherASlugIsTaken() {
        Composition published = new Composition(alice, "Song", "pattern", 100);
        published.setSlug("taken123");
        compositions.save(published);

        assertThat(compositions.existsBySlug("taken123")).isTrue();
        assertThat(compositions.existsBySlug("free-slug")).isFalse();
    }

    // --- findBySlugAndIsPublicTrue ------------------------------------------

    @Test
    void findBySlugAndIsPublicTrue_findsAPublishedCompositionByItsSlug() {
        Composition published = new Composition(alice, "Public song", "pattern", 100);
        published.setSlug("share123");
        published.setPublic(true);
        compositions.save(published);

        assertThat(compositions.findBySlugAndIsPublicTrue("share123"))
                .isPresent()
                .get()
                .extracting(Composition::getTitle)
                .isEqualTo("Public song");
    }

    @Test
    void findBySlugAndIsPublicTrue_missesOnceUnpublishedEvenThoughTheSlugRemains() {
        // mirrors CompositionService#unpublish: slug is kept, isPublic flips to false
        Composition unpublished = new Composition(alice, "Unpublished song", "pattern", 100);
        unpublished.setSlug("share123");
        unpublished.setPublic(false);
        compositions.save(unpublished);

        assertThat(compositions.findBySlugAndIsPublicTrue("share123")).isEmpty();
    }

    @Test
    void findBySlugAndIsPublicTrue_missesForAnUnknownSlug() {
        assertThat(compositions.findBySlugAndIsPublicTrue("no-such-slug")).isEmpty();
    }

    // --- findByIsPublicTrueOrderByUpdatedAtDesc (explore feed) --------------

    @Test
    void findByIsPublicTrueOrderByUpdatedAtDesc_returnsOnlyPublishedCompositions() {
        Composition published = new Composition(alice, "Published", "pattern", 100);
        published.setPublic(true);
        compositions.save(published);
        compositions.save(new Composition(bob, "Private", "pattern", 100));

        List<Composition> result =
                compositions.findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(0, 10));

        assertThat(result).extracting(Composition::getTitle).containsExactly("Published");
    }

    @Test
    void findByIsPublicTrueOrderByUpdatedAtDesc_respectsThePageSize() {
        for (int i = 0; i < 3; i++) {
            Composition published = new Composition(alice, "Song " + i, "pattern", 100);
            published.setPublic(true);
            compositions.save(published);
        }

        List<Composition> result =
                compositions.findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    // --- findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc (public profile) ----

    @Test
    void findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc_excludesPrivateCompositionsAndOtherOwners() {
        Composition alicePublic = new Composition(alice, "Alice public", "pattern", 100);
        alicePublic.setPublic(true);
        compositions.save(alicePublic);
        compositions.save(new Composition(alice, "Alice private", "pattern", 100));
        Composition bobPublic = new Composition(bob, "Bob public", "pattern", 100);
        bobPublic.setPublic(true);
        compositions.save(bobPublic);

        List<Composition> result = compositions.findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(alice);

        assertThat(result).extracting(Composition::getTitle).containsExactly("Alice public");
    }

    // --- findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc (following feed) ---

    @Test
    void findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc_onlyIncludesPublicPostsFromGivenOwners() {
        User carol = users.save(new User("carol", "carol@example.com", "hashed-pw"));

        Composition alicePublic = new Composition(alice, "Alice public", "pattern", 100);
        alicePublic.setPublic(true);
        compositions.save(alicePublic);
        compositions.save(new Composition(bob, "Bob private", "pattern", 100));
        Composition carolPublic = new Composition(carol, "Carol public (not followed)", "pattern", 100);
        carolPublic.setPublic(true);
        compositions.save(carolPublic);

        List<Composition> result =
                compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(
                        List.of(alice.getId(), bob.getId()), PageRequest.of(0, 10));

        assertThat(result).extracting(Composition::getTitle).containsExactly("Alice public");
    }
}