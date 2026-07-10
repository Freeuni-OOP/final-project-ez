package com.algorythm.dto;

import com.algorythm.model.User;
import java.time.Instant;

/**
 * A compact public view of a user for lists (search results, followers,
 * following). isFollowing is true only when the request is from a logged-in user
 * who already follows this user.
 */
public record UserSummaryResponse(String username, Instant joinedAt, boolean isFollowing) {

    public static UserSummaryResponse of(User user, boolean isFollowing) {
        return new UserSummaryResponse(user.getUsername(), user.getCreatedAt(), isFollowing);
    }
}
