package com.vibes.iam.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration tests.
 * This configuration provides containerized PostgreSQL and Redis instances for
 * testing.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    /**
     * PostgreSQL container for database integration tests.
     * Uses Spring Boot's @ServiceConnection for automatic configuration.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/test-data.sql") // Optional: initial test data
                .withReuse(true); // Reuse container across test runs for faster execution
    }

    /**
     * Redis container for session and caching integration tests.
     * Uses Spring Boot's @ServiceConnection for automatic configuration.
     */
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
    }
}