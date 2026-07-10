package com.algorythm.controller;

import com.algorythm.dto.ChangeEmailRequest;
import com.algorythm.dto.ChangePasswordRequest;
import com.algorythm.dto.DeleteAccountRequest;
import com.algorythm.dto.UserResponse;
import com.algorythm.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The current user's account settings. Lives under /api/** so a valid JWT is
 * required; the user is always taken from the token, never the request body.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(
                authentication.getName(), request.currentPassword(), request.newPassword());
    }

    @PutMapping("/email")
    public UserResponse changeEmail(
            Authentication authentication, @Valid @RequestBody ChangeEmailRequest request) {
        return accountService.changeEmail(
                authentication.getName(), request.currentPassword(), request.email());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            Authentication authentication, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(authentication.getName(), request.currentPassword());
    }
}
