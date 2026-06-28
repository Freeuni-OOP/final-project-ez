package com.algorythm.dto;

import com.algorythm.model.User;
import java.time.Instant;

/**
 * Public view of a user. Never includes the password hash or other internal
 * fields, so it is safe to return from any endpoint.
 */
public record UserResponse(Long id, String username, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
