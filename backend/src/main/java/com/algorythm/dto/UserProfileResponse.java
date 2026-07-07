package com.algorythm.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public profile for a user: basic public info, follow stats, and their published
 * compositions. No email or other private fields. isFollowing is true only when
 * the request is from a logged-in user who follows this profile.
 */
public record UserProfileResponse(
        String username,
        Instant joinedAt,
        long followerCount,
        long followingCount,
        boolean isFollowing,
        List<PublicCompositionResponse> compositions) {
}
