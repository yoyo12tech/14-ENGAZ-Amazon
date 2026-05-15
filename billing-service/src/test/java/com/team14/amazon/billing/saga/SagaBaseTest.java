package com.team14.amazon.billing.saga;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.contracts.feign.UserServiceClient;
import com.team14.amazon.contracts.dto.UserDTO;
@SpringBootTest
@Testcontainers
public abstract class SagaBaseTest {

    // --- Containers (shared across all test classes) ---

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("amazondb-billing")
            .withUsername("billing")
            .withPassword("billing");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-management");

    // --- Wire containers into Spring's property sources ---

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);

        // Disable MongoDB and Redis for tests (not needed)
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/test");
        registry.add("spring.data.redis.host", () -> "localhost");
    }

    @Autowired
    protected TransactionRepository transactionRepository;

    @MockitoBean
    protected UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        // Clean slate before every test
        transactionRepository.deleteAll();

        // Default mock: UserServiceClient always returns a valid user
        when(userServiceClient.getUser(anyLong()))
                .thenReturn(new UserDTO(1L, "Test User", "testuser@email.com", "01000000000", "USER", "ACTIVE"));
    }
}