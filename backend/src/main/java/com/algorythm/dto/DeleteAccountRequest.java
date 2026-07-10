package com.algorythm.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for deleting your account. The current password is required to confirm it's you. */
public record DeleteAccountRequest(
        @NotBlank String currentPassword) {
}
