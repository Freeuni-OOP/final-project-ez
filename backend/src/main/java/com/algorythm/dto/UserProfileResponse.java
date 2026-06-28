package com.algorythm.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public profile for a user: basic public info plus their published compositions.
 * No email or other private fields.
 */
public record UserProfileResponse(
        String username,
        Instant joinedAt,
        List<PublicCompositionResponse> compositions) {
}
