package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionLikeRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * End-to-end coverage of liking/unliking against a real Postgres (via
 * AbstractIntegrationTest): runs through the real LikeService + repositories,
 * so duplicate prevention is proven against the DB's (user_id, composition_id)
 * key, not just mocked wiring. @Transactional rolls each test's writes back.
 */
@Transactional
class LikeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private LikeService likeService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;
    @Autowired private CompositionLikeRepository likeRepository;

    private User alice;
    private User bob;
    private Composition publicComposition;
    private Composition privateComposition;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob@example.com", "hashed-pw"));

        Composition published = new Composition(alice, "Public song", "pattern", 100);
        published.setPublic(true);
        published.setSlug("liketest1");
        publicComposition = compositionRepository.save(published);

        privateComposition =
                compositionRepository.save(new Composition(alice, "Private song", "pattern", 100));
    }

    @Test
    void like_thenUnlike_roundTrips() {
        likeService.like("bob", publicComposition.getId());
        assertThat(likeRepository.countByIdCompositionId(publicComposition.getId())).isEqualTo(1);

        likeService.unlike("bob", publicComposition.getId());
        assertThat(likeRepository.countByIdCompositionId(publicComposition.getId())).isEqualTo(0);
    }

    @Test
    void like_isIdempotent_likingTwiceCreatesNoDuplicate() {
        likeService.like("bob", publicComposition.getId());
        likeService.like("bob", publicComposition.getId());

        assertThat(likeRepository.countByIdCompositionId(publicComposition.getId())).isEqualTo(1);
    }

    @Test
    void unlike_isHarmlessWhenNothingWasEverLiked() {
        assertDoesNotThrow(() -> likeService.unlike("bob", publicComposition.getId()));
        assertThat(likeRepository.countByIdCompositionId(publicComposition.getId())).isEqualTo(0);
    }

    @Test
    void like_rejectsAPrivateComposition() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> likeService.like("bob", privateComposition.getId()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(likeRepository.countByIdCompositionId(privateComposition.getId())).isEqualTo(0);
    }

    @Test
    void like_rejectsANonexistentComposition() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> likeService.like("bob", 999_999L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void likeCountAndLikedByMe_areCorrectForTheAskingUserAndFalseForAnonymous() {
        likeService.like("bob", publicComposition.getId());

        PublicCompositionResponse asLiker = likeService.toResponse(publicComposition, "bob");
        assertThat(asLiker.likeCount()).isEqualTo(1);
        assertThat(asLiker.likedByMe()).isTrue();

        PublicCompositionResponse asOwnerWhoDidntLike =
                likeService.toResponse(publicComposition, "alice");
        assertThat(asOwnerWhoDidntLike.likeCount()).isEqualTo(1);
        assertThat(asOwnerWhoDidntLike.likedByMe()).isFalse();

        PublicCompositionResponse asAnonymous = likeService.toResponse(publicComposition, null);
        assertThat(asAnonymous.likeCount()).isEqualTo(1);
        assertThat(asAnonymous.likedByMe()).isFalse();
    }

    @Test
    void likeCountReflectsMultipleLikers() {
        User carol = userRepository.save(new User("carol", "carol@example.com", "hashed-pw"));

        likeService.like("bob", publicComposition.getId());
        likeService.like("carol", publicComposition.getId());

        assertThat(likeService.toResponse(publicComposition, null).likeCount()).isEqualTo(2);
    }
}