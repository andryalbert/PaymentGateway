package com.andryalbert.fraud.controller;

import com.andryalbert.fraud.entity.FraudCheck;
import com.andryalbert.fraud.entity.FraudStatus;
import com.andryalbert.fraud.repository.FraudCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FraudControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("frauddb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FraudCheckRepository fraudCheckRepository;

    @BeforeEach
    void setUp() {
        fraudCheckRepository.deleteAll();
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/fraud";
    }

    @Test
    void createFraudCheck_WhenValidData_ShouldSaveToDatabase() {
        FraudCheck fraudCheck = new FraudCheck(
            "FC-123", "pay-123", "cust-123", "4111111111111111",
            new BigDecimal("100.00"), "USD", "192.168.1.1", "Mozilla/5.0", "US"
        );
        fraudCheck.setStatus(FraudStatus.APPROVED);
        fraudCheck.setRiskScore(10);
        fraudCheck.setReason("No suspicious activity");
        fraudCheck.setCreatedAt(LocalDateTime.now());

        ResponseEntity<FraudCheck> response = restTemplate.postForEntity(
            getBaseUrl(),
            fraudCheck,
            FraudCheck.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FC-123", response.getBody().getCheckId());
        assertEquals("pay-123", response.getBody().getPaymentId());
        assertEquals(FraudStatus.APPROVED, response.getBody().getStatus());

        // Verify data was actually saved to database
        assertTrue(fraudCheckRepository.findByCheckId("FC-123").isPresent());
    }

    @Test
    void getFraudCheck_WhenCheckExists_ShouldReturnCheck() {
        FraudCheck fraudCheck = new FraudCheck(
            "FC-456", "pay-456", "cust-456", "4111111111111111",
            new BigDecimal("100.00"), "USD", "192.168.1.1", "Mozilla/5.0", "US"
        );
        fraudCheck.setStatus(FraudStatus.APPROVED);
        fraudCheck.setRiskScore(10);
        fraudCheck.setReason("No suspicious activity");
        fraudCheck.setCreatedAt(LocalDateTime.now());
        fraudCheckRepository.save(fraudCheck);

        ResponseEntity<FraudCheck> response = restTemplate.getForEntity(
            getBaseUrl() + "/check/FC-456",
            FraudCheck.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FC-456", response.getBody().getCheckId());
        assertEquals("pay-456", response.getBody().getPaymentId());
        assertEquals(FraudStatus.APPROVED, response.getBody().getStatus());
    }

    @Test
    void getFraudCheck_WhenCheckNotExists_ShouldReturn404() {
        ResponseEntity<FraudCheck> response = restTemplate.getForEntity(
            getBaseUrl() + "/check/FC-NONEXISTENT",
            FraudCheck.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFraudCheckByPaymentId_WhenCheckExists_ShouldReturnCheck() {
        FraudCheck fraudCheck = new FraudCheck(
            "FC-789", "pay-789", "cust-789", "4111111111111111",
            new BigDecimal("500.00"), "EUR", "10.0.0.1", "Chrome", "FR"
        );
        fraudCheck.setStatus(FraudStatus.REJECTED);
        fraudCheck.setRiskScore(80);
        fraudCheck.setReason("High risk detected");
        fraudCheck.setCreatedAt(LocalDateTime.now());
        fraudCheckRepository.save(fraudCheck);

        ResponseEntity<FraudCheck> response = restTemplate.getForEntity(
            getBaseUrl() + "/payment/pay-789",
            FraudCheck.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FC-789", response.getBody().getCheckId());
        assertEquals("pay-789", response.getBody().getPaymentId());
        assertEquals(FraudStatus.REJECTED, response.getBody().getStatus());
    }

    @Test
    void getFraudCheckByPaymentId_WhenCheckNotExists_ShouldReturn404() {
        ResponseEntity<FraudCheck> response = restTemplate.getForEntity(
            getBaseUrl() + "/payment/pay-NONEXISTENT",
            FraudCheck.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFraudChecksByCustomer_WhenChecksExist_ShouldReturnList() {
        FraudCheck check1 = new FraudCheck(
            "FC-001", "pay-001", "cust-001", "4111111111111111",
            new BigDecimal("100.00"), "USD", "192.168.1.1", "Mozilla", "US"
        );
        check1.setStatus(FraudStatus.APPROVED);
        check1.setRiskScore(5);
        check1.setCreatedAt(LocalDateTime.now());

        FraudCheck check2 = new FraudCheck(
            "FC-002", "pay-002", "cust-001", "4111111111111111",
            new BigDecimal("200.00"), "USD", "192.168.1.2", "Chrome", "US"
        );
        check2.setStatus(FraudStatus.MANUAL_REVIEW);
        check2.setRiskScore(45);
        check2.setCreatedAt(LocalDateTime.now());

        fraudCheckRepository.save(check1);
        fraudCheckRepository.save(check2);

        ResponseEntity<List<FraudCheck>> response = restTemplate.exchange(
            getBaseUrl() + "/customer/cust-001",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<FraudCheck>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("cust-001", response.getBody().get(0).getCustomerId());
        assertEquals("cust-001", response.getBody().get(1).getCustomerId());
    }

    @Test
    void getFraudChecksByCustomer_WhenNoChecksExist_ShouldReturnEmptyList() {
        ResponseEntity<List<FraudCheck>> response = restTemplate.exchange(
            getBaseUrl() + "/customer/cust-NONEXISTENT",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<FraudCheck>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void health_ShouldReturnHealthyMessage() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            getBaseUrl() + "/health",
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Fraud Detection Service is healthy", response.getBody());
    }

    @Test
    void fullFlow_CreateThenRetrieve_ShouldWorkEndToEnd() {
        FraudCheck fraudCheck = new FraudCheck(
            "FC-FULL", "pay-full", "cust-full", "4111111111111111",
            new BigDecimal("1000.00"), "USD", "192.168.1.100", "TestAgent", "US"
        );
        fraudCheck.setStatus(FraudStatus.MANUAL_REVIEW);
        fraudCheck.setRiskScore(50);
        fraudCheck.setReason("Manual review required");
        fraudCheck.setCreatedAt(LocalDateTime.now());

        // Step 1: Create via POST
        ResponseEntity<FraudCheck> createResponse = restTemplate.postForEntity(
            getBaseUrl(),
            fraudCheck,
            FraudCheck.class
        );
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals("FC-FULL", createResponse.getBody().getCheckId());

        // Step 2: Retrieve via GET by checkId
        ResponseEntity<FraudCheck> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/check/FC-FULL",
            FraudCheck.class
        );
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("FC-FULL", getResponse.getBody().getCheckId());
        assertEquals(FraudStatus.MANUAL_REVIEW, getResponse.getBody().getStatus());

        // Step 3: Retrieve via GET by paymentId
        ResponseEntity<FraudCheck> paymentResponse = restTemplate.getForEntity(
            getBaseUrl() + "/payment/pay-full",
            FraudCheck.class
        );
        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());
        assertEquals("FC-FULL", paymentResponse.getBody().getCheckId());

        // Step 4: Retrieve via GET by customerId
        ResponseEntity<List<FraudCheck>> customerResponse = restTemplate.exchange(
            getBaseUrl() + "/customer/cust-full",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<FraudCheck>>() {}
        );
        assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
        assertEquals(1, customerResponse.getBody().size());
        assertEquals("FC-FULL", customerResponse.getBody().get(0).getCheckId());

        // Step 5: Verify data persisted in database
        assertTrue(fraudCheckRepository.findByCheckId("FC-FULL").isPresent());
        assertTrue(fraudCheckRepository.findByPaymentId("pay-full").isPresent());
        assertEquals(1, fraudCheckRepository.findByCustomerId("cust-full").size());
    }
}
