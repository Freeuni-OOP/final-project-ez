package com.algorythm.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Exercises JwtService directly (no Spring context) against real jjwt encoding,
 * since the whole point of this class is real cryptographic round-tripping.
 */
class JwtServiceTest {

    // HS256 needs >= 32 bytes; repeating a short string keeps these readable
    // while guaranteeing length.
    private static final String SECRET = "test-jwt-secret-".repeat(3);
    private static final String OTHER_SECRET = "other-jwt-secret-".repeat(3);

    private static JwtService service(long expirationMs) {
        return new JwtService(SECRET, expirationMs);
    }

    @Test
    void generateToken_roundTripsToTheOriginalUsername() {
        JwtService jwtService = service(60_000);

        String token = jwtService.generateToken("alice");

        assertThat(jwtService.validateAndGetUsername(token)).isEqualTo("alice");
    }

    @Test
    void validateAndGetUsername_rejectsAnExpiredToken() {
        // Negative expiration puts the "exp" claim in the past the instant the
        // token is minted, no need to sleep the test.
        JwtService expiredIssuer = service(-1_000);
        JwtService validator = service(60_000);

        String expiredToken = expiredIssuer.generateToken("alice");

        assertThat(validator.validateAndGetUsername(expiredToken)).isNull();
    }

    @Test
    void validateAndGetUsername_rejectsATamperedToken() {
        JwtService jwtService = service(60_000);
        String token = jwtService.generateToken("alice");
        char lastChar = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (lastChar == 'a' ? 'b' : 'a');

        assertThat(jwtService.validateAndGetUsername(tampered)).isNull();
    }

    @Test
    void validateAndGetUsername_rejectsATokenSignedWithADifferentKey() {
        JwtService signer = new JwtService(OTHER_SECRET, 60_000);
        JwtService validator = service(60_000);

        String token = signer.generateToken("alice");

        assertThat(validator.validateAndGetUsername(token)).isNull();
    }

    @Test
    void validateAndGetUsername_rejectsMalformedTokens() {
        JwtService jwtService = service(60_000);

        assertThat(jwtService.validateAndGetUsername("not-a-jwt")).isNull();
        assertThat(jwtService.validateAndGetUsername("")).isNull();
        assertThat(jwtService.validateAndGetUsername("a.b.c")).isNull();
    }
}