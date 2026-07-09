package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end coverage of the following feed against a real Postgres, running
 * through the real FeedService + repositories - nothing mocked. Proves: only
 * the public work of people the viewer follows shows up, newest first, and
 * nothing from people they don't follow (or private work from people they
 * do). @Transactional rolls each test's writes back.
 */
@Transactional
class FeedServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private FeedService feedService;
    @Autowired private FollowService followService;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob@example.com", "hashed-pw"));
        carol = userRepository.save(new User("carol", "carol@example.com", "hashed-pw"));
    }

    @Test
    void following_returnsOnlyPublicWorkFromFollowedUsers_newestFirst() {
        followService.follow("alice", "bob");

        Composition bobOlder = publish(bob, "Bob older", "feedslugold");
        Composition bobNewer = publish(bob, "Bob newer", "feedslugnew");
        compositionRepository.save(new Composition(bob, "Bob private draft", "pattern", 100));
        publish(carol, "Carol public (not followed)", "feedslugcarol");

        // touch "bobOlder" last so it's the most recently updated
        bobOlder.setTitle("Bob older, edited");
        compositionRepository.saveAndFlush(bobOlder);

        List<PublicCompositionResponse> result = feedService.following("alice", 0, 20);

        assertThat(result).extracting(PublicCompositionResponse::slug)
                .containsExactly("feedslugold", "feedslugnew");
    }

    @Test
    void following_isEmptyWhenTheViewerFollowsNobody() {
        publish(bob, "Bob public", "feedslugalone");

        assertThat(feedService.following("alice", 0, 20)).isEmpty();
    }

    @Test
    void following_respectsPageAndSizeParameters() {
        followService.follow("alice", "bob");
        Composition first = publish(bob, "First", "feedp1");
        Composition second = publish(bob, "Second", "feedp2");
        Composition third = publish(bob, "Third", "feedp3");
        second.setTitle("Second, edited");
        compositionRepository.saveAndFlush(second);
        third.setTitle("Third, edited");
        compositionRepository.saveAndFlush(third);

        List<PublicCompositionResponse> firstPage = feedService.following("alice", 0, 2);
        assertThat(firstPage).extracting(PublicCompositionResponse::slug)
                .containsExactly("feedp3", "feedp2");

        List<PublicCompositionResponse> secondPage = feedService.following("alice", 1, 2);
        assertThat(secondPage).extracting(PublicCompositionResponse::slug).containsExactly("feedp1");
    }

    @Test
    void following_stopsIncludingSomeoneOnceUnfollowed() {
        followService.follow("alice", "bob");
        publish(bob, "Bob public", "feedunfollow1");
        assertThat(feedService.following("alice", 0, 20)).hasSize(1);

        followService.unfollow("alice", "bob");

        assertThat(feedService.following("alice", 0, 20)).isEmpty();
    }

    private Composition publish(User owner, String title, String slug) {
        Composition composition = new Composition(owner, title, "pattern", 100);
        composition.setSlug(slug);
        composition.setPublic(true);
        return compositionRepository.save(composition);
    }
}
