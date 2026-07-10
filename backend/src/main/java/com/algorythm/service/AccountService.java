package com.algorythm.service;

import com.algorythm.dto.UserResponse;
import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages the current user's own account: password, email, and deletion. Every
 * method operates on the authenticated user, so one user can only ever change
 * their own account, and each re-checks the current password first.
 */
@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = currentUser(username);
        verifyPassword(user, currentPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public UserResponse changeEmail(String username, String currentPassword, String newEmail) {
        User user = currentUser(username);
        verifyPassword(user, currentPassword);
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && users.existsByEmail(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        user.setEmail(newEmail);
        return UserResponse.from(user);
    }

    /** Deletes the account. FK cascades remove everything tied to the user. */
    @Transactional
    public void deleteAccount(String username, String currentPassword) {
        User user = currentUser(username);
        verifyPassword(user, currentPassword);
        users.delete(user);
    }

    private void verifyPassword(User user, String password) {
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Current password is incorrect");
        }
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
