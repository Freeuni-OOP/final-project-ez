package com.algorythm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algorythm.dto.AuthResponse;
import com.algorythm.dto.LoginRequest;
import com.algorythm.dto.RegisterRequest;
import com.algorythm.dto.UserResponse;
import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import com.algorythm.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for registration/login/"who am I" logic. The repository, password
 * encoder, and JwtService are all mocked so these run without a database or real
 * hashing/signing.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, jwtService);
    }

    // --- register --------------------------------------------------

    @Test
    void register_savesUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-pw");
    }

    @Test
    void register_rejectsAlreadyTakenUsername() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(users.existsByUsername("alice")).thenReturn(true);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(users, never()).save(any());
    }

    @Test
    void register_rejectsAlreadyRegisteredEmail() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.existsByEmail("alice@example.com")).thenReturn(true);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(users, never()).save(any());
    }

    // --- login -------------------------------------------------------

    @Test
    void login_succeedsWithUsernameAndReturnsSignedToken() {
        User user = new User("alice", "alice@example.com", "hashed-pw");
        LoginRequest request = new LoginRequest("alice", "password123");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("alice");
    }

    @Test
    void login_fallsBackToEmailWhenUsernameLookupMisses() {
        User user = new User("alice", "alice@example.com", "hashed-pw");
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        when(users.findByUsername("alice@example.com")).thenReturn(Optional.empty());
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.user().username()).isEqualTo("alice");
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User("alice", "alice@example.com", "hashed-pw");
        LoginRequest request = new LoginRequest("alice", "wrong-password");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-pw")).thenReturn(false);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_rejectsUnknownIdentifier() {
        LoginRequest request = new LoginRequest("ghost", "password123");
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());
        when(users.findByEmail("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // --- getByUsername -------------------------------------------------

    @Test
    void getByUsername_returnsThePublicView() {
        User user = new User("alice", "alice@example.com", "hashed-pw");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));

        UserResponse response = authService.getByUsername("alice");

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void getByUsername_rejectsUnknownUsername() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> authService.getByUsername("ghost"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}