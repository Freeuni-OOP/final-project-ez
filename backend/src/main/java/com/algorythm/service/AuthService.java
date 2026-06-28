package com.algorythm.service;

import com.algorythm.dto.AuthResponse;
import com.algorythm.dto.LoginRequest;
import com.algorythm.dto.RegisterRequest;
import com.algorythm.dto.UserResponse;
import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import com.algorythm.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Registration, login, and "who am I" logic. Passwords are only ever stored hashed. */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Create a user after checking the username/email are free; returns the public view. */
    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (users.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()));
        return UserResponse.from(users.save(user));
    }

    /** Verify credentials (username or email) and return a signed JWT. */
    public AuthResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username())
                .or(() -> users.findByEmail(request.username()))
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUsername());
        return AuthResponse.bearer(token, UserResponse.from(user));
    }

    /** The user behind the current token (by username = JWT subject). */
    public UserResponse getByUsername(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        return UserResponse.from(user);
    }
}
