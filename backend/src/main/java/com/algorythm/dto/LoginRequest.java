package com.algorythm.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/login. The identifier may be a username or an email. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
