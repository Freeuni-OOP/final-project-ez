package com.algorythm.service;

import com.algorythm.model.Follow;
import com.algorythm.model.FollowId;
import com.algorythm.model.User;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Following between users. Handles follow/unfollow (idempotent, no self-follow)
 * and the counts / isFollowing flag shown on public profiles.
 */
@Service
public class FollowService {

    private final FollowRepository follows;
    private final UserRepository users;

    public FollowService(FollowRepository follows, UserRepository users) {
        this.follows = follows;
        this.users = users;
    }

    /** Follow the given user. Following again is a no-op; following yourself is rejected. */
    @Transactional
    public void follow(String followerUsername, String targetUsername) {
        User follower = requireUser(followerUsername);
        User target = targetUser(targetUsername);
        if (follower.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't follow yourself");
        }
        FollowId id = new FollowId(follower.getId(), target.getId());
        if (!follows.existsById(id)) {
            follows.save(new Follow(follower.getId(), target.getId()));
        }
    }

    /** Stop following the given user. Unfollowing someone you don't follow is a no-op. */
    @Transactional
    public void unfollow(String followerUsername, String targetUsername) {
        User follower = requireUser(followerUsername);
        User target = targetUser(targetUsername);
        follows.deleteByIdFollowerIdAndIdFollowingId(follower.getId(), target.getId());
    }

    /** How many users follow this user. */
    @Transactional(readOnly = true)
    public long followerCount(Long userId) {
        return follows.countByIdFollowingId(userId);
    }

    /** How many users this user follows. */
    @Transactional(readOnly = true)
    public long followingCount(Long userId) {
        return follows.countByIdFollowerId(userId);
    }

    /** Whether viewer (nullable) follows the given user. */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long viewerId, Long targetId) {
        return viewerId != null
                && follows.existsByIdFollowerIdAndIdFollowingId(viewerId, targetId);
    }

    private User targetUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
