package com.algorythm.dto;

/** Response for a successful login: the JWT plus the public user view. */
public record AuthResponse(String token, String tokenType, UserResponse user) {

    public static AuthResponse bearer(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
