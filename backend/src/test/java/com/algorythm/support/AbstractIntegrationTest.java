package com.algorythm.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for any test that needs a real database.
 *
 * This starts an actual PostgreSQL 16 container via Docker (same image the
 * "db" service in docker-compose.yml uses), points the Spring datasource at
 * it, and lets Flyway run the real migrations from
 * src/main/resources/db/migration against it on context startup. Nothing is
 * faked or swapped for an in-memory database, so Postgres-only column types
 * (BIGSERIAL, TIMESTAMPTZ, ...) and the actual migration history get
 * exercised exactly like they would in production.
 *
 * Every other DB-touching test should extend this instead of rolling its own
 * container/wiring.
 *
 * Uses MOCK web environment (not NONE) because SecurityConfig's filter chain
 * bean depends on an HttpSecurity bean that Spring Boot only auto-configures
 * for a servlet web application context.
 *
 * Requires a working Docker daemon on the machine running the tests
 * (Docker Desktop, Colima, etc.) - Testcontainers talks to it directly.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("algorythm_test")
                    .withUsername("algorythm")
                    .withPassword("algorythm");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
