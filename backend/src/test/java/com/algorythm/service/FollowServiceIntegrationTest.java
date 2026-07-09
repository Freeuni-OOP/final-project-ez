package com.algorythm.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


/**
 * End-to-end coverage of follow/unfollow against a real Postgres (via
 * AbstractIntegrationTest): runs through the real FollowService + repository,
 * so duplicate prevention is proven against the DB's (follower_id,
 * following_id) key, not just mocked wiring. @Transactional rolls each test's
 * writes back.
 */
@Transactional
class FollowServiceIntegrationTest extends AbstractIntegrationTest {


    @Autowired private FollowService followService;
    @Autowired private UserRepository userRepository;


    private User alice;
    private User bob;


    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hashed-pw"));
        bob = userRepository.save(new User("bob", "bob@example.com", "hashed-pw"));
    }


    @Test
    void follow_thenUnfollow_roundTrips() {
        followService.follow("alice", "bob");
        assertThat(followService.followerCount(bob.getId())).isEqualTo(1);
        assertThat(followService.isFollowing(alice.getId(), bob.getId())).isTrue();


        followService.unfollow("alice", "bob");
        assertThat(followService.followerCount(bob.getId())).isEqualTo(0);
        assertThat(followService.isFollowing(alice.getId(), bob.getId())).isFalse();
    }


    @Test
    void follow_isIdempotent_followingTwiceCreatesNoDuplicate() {
        followService.follow("alice", "bob");
        followService.follow("alice", "bob");


        assertThat(followService.followerCount(bob.getId())).isEqualTo(1);
    }


    @Test
    void unfollow_isHarmlessWhenNeverFollowed() {
        assertDoesNotThrow(() -> followService.unfollow("alice", "bob"));
        assertThat(followService.followerCount(bob.getId())).isEqualTo(0);
    }


    @Test
    void follow_rejectsFollowingYourself() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> followService.follow("alice", "alice"));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(followService.followerCount(alice.getId())).isEqualTo(0);
    }


    @Test
    void follow_rejectsAnUnknownTargetUser() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> followService.follow("alice", "ghost"));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    void followerAndFollowingCountsAndIsFollowing_areCorrectForEveryoneInvolved() {
        User carol = userRepository.save(new User("carol", "carol@example.com", "hashed-pw"));
        followService.follow("alice", "bob");
        followService.follow("carol", "bob");


        assertThat(followService.followerCount(bob.getId())).isEqualTo(2);
        assertThat(followService.followingCount(alice.getId())).isEqualTo(1);
        assertThat(followService.followingCount(bob.getId())).isEqualTo(0);


        assertThat(followService.isFollowing(alice.getId(), bob.getId())).isTrue();
        assertThat(followService.isFollowing(bob.getId(), alice.getId())).isFalse();
        assertThat(followService.isFollowing(null, bob.getId())).isFalse();
    }
}



