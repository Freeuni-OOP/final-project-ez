package com.algorythm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body for changing your email. The current password is required to confirm it's you. */
public record ChangeEmailRequest(
        @NotBlank String currentPassword,
        @NotBlank @Email String email) {
}
