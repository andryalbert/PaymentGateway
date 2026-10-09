package com.andryalbert.payment.controller;

import com.andryalbert.payment.entity.Payment;
import com.andryalbert.payment.entity.PaymentStatus;
import com.andryalbert.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("paymentdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_ShouldReturnCreatedPayment() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.50,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "customerId": "CUST-001",
                "description": "Test payment",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "123 Main St",
                "billingCity": "New York",
                "billingCountry": "US",
                "billingZip": "10001",
                "idempotencyKey": "idemp-test-001"
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isString())
                .andExpect(jsonPath("$.paymentId").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp-test-001"))
                .andExpect(jsonPath("$.cardHolder").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.merchantId").value("MERCHANT-001"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.status").value("VALIDATING"))
                .andExpect(jsonPath("$.cardNumber").value(startsWith("****-****-****-")));

        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertEquals("idemp-test-001", payments.get(0).getIdempotencyKey());
    }

    @Test
    void createPayment_ShouldReturn400_WhenIdempotencyKeyExists() throws Exception {
        Payment existingPayment = paymentRepository.save(new Payment(
            "PAY-EXISTING", "idemp-duplicate", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.50,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "customerId": "CUST-001",
                "description": "Test payment",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "123 Main St",
                "billingCity": "New York",
                "billingCountry": "US",
                "billingZip": "10001",
                "idempotencyKey": "idemp-duplicate"
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getPayment_ShouldReturnPayment_WhenPaymentExists() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getPaymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp-001"))
                .andExpect(jsonPath("$.cardHolder").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.merchantId").value("MERCHANT-001"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"));
    }

    @Test
    void getPayment_ShouldReturn500_WhenPaymentNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{paymentId}", "PAY-NONEXISTENT"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getPaymentsByMerchant_ShouldReturnPayments_WhenPaymentsExist() throws Exception {
        Payment payment1 = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment 1", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        Payment payment2 = paymentRepository.save(new Payment(
            "PAY-TEST-002", "idemp-002", "4111111111111111", "Jane Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-001", "Test payment 2", "CUST-002", "192.168.1.2",
            "Mozilla/5.0", "456 Oak St", "Los Angeles", "US", "90001"
        ));

        mockMvc.perform(get("/api/v1/payments/merchant/{merchantId}", "MERCHANT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].merchantId", everyItem(is("MERCHANT-001"))))
                .andExpect(jsonPath("$[*].paymentId", containsInAnyOrder(
                    payment1.getPaymentId(),
                    payment2.getPaymentId()
                )));
    }

    @Test
    void getPaymentsByMerchant_ShouldReturnEmptyList_WhenNoPayments() throws Exception {
        mockMvc.perform(get("/api/v1/payments/merchant/{merchantId}", "MERCHANT-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPaymentsByCustomer_ShouldReturnPayments_WhenPaymentsExist() throws Exception {
        Payment payment1 = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment 1", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        Payment payment2 = paymentRepository.save(new Payment(
            "PAY-TEST-002", "idemp-002", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-002", "Test payment 2", "CUST-001", "192.168.1.2",
            "Mozilla/5.0", "456 Oak St", "Los Angeles", "US", "90001"
        ));

        mockMvc.perform(get("/api/v1/payments/customer/{customerId}", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].customerId", everyItem(is("CUST-001"))))
                .andExpect(jsonPath("$[*].paymentId", containsInAnyOrder(
                    payment1.getPaymentId(),
                    payment2.getPaymentId()
                )));
    }

    @Test
    void getPaymentsByCustomer_ShouldReturnEmptyList_WhenNoPayments() throws Exception {
        mockMvc.perform(get("/api/v1/payments/customer/{customerId}", "CUST-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void refundPayment_ShouldRefundPayment_WhenPaymentIsSettled() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));
        payment.setStatus(PaymentStatus.SETTLED);
        paymentRepository.save(payment);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/refund", payment.getPaymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Payment refundedPayment = paymentRepository.findByPaymentId(payment.getPaymentId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, refundedPayment.getStatus());
    }

    @Test
    void refundPayment_ShouldReturn500_WhenPaymentIsNotSettledOrCaptured() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/refund", payment.getPaymentId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void refundPayment_ShouldReturn500_WhenPaymentNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{paymentId}/refund", "PAY-NONEXISTENT"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancelPayment_ShouldCancelPayment_WhenPaymentIsNotSettledOrRefunded() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", payment.getPaymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Payment cancelledPayment = paymentRepository.findByPaymentId(payment.getPaymentId()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, cancelledPayment.getStatus());
    }

    @Test
    void cancelPayment_ShouldReturn500_WhenPaymentIsSettled() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));
        payment.setStatus(PaymentStatus.SETTLED);
        paymentRepository.save(payment);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", payment.getPaymentId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancelPayment_ShouldReturn500_WhenPaymentIsRefunded() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-TEST-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", payment.getPaymentId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancelPayment_ShouldReturn500_WhenPaymentNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{paymentId}/cancel", "PAY-NONEXISTENT"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void health_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment Service is healthy"));
    }

    @Test
    void fullFlow_CreateAndRetrievePayment() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "Full Flow User",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 500.00,
                "currency": "USD",
                "merchantId": "MERCHANT-FLOW",
                "customerId": "CUST-FLOW",
                "description": "Full flow test",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "123 Main St",
                "billingCity": "New York",
                "billingCountry": "US",
                "billingZip": "10001",
                "idempotencyKey": "idemp-flow-001"
            }
            """;

        String responseContent = mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = objectMapper.readTree(responseContent).get("paymentId").asText();

        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.cardHolder").value("Full Flow User"))
                .andExpect(jsonPath("$.amount").value(500.00));

        mockMvc.perform(get("/api/v1/payments/merchant/{merchantId}", "MERCHANT-FLOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId));

        mockMvc.perform(get("/api/v1/payments/customer/{customerId}", "CUST-FLOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId));

        List<Payment> allPayments = paymentRepository.findAll();
        assertEquals(1, allPayments.size());
    }

    @Test
    void multiplePayments_ShouldBeStoredIndependently() throws Exception {
        String paymentRequest1 = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "User One",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.00,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "customerId": "CUST-001",
                "description": "Payment 1",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "123 Main St",
                "billingCity": "New York",
                "billingCountry": "US",
                "billingZip": "10001",
                "idempotencyKey": "idemp-001"
            }
            """;

        String paymentRequest2 = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "User Two",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 200.00,
                "currency": "EUR",
                "merchantId": "MERCHANT-002",
                "customerId": "CUST-002",
                "description": "Payment 2",
                "ipAddress": "192.168.1.2",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "456 Oak St",
                "billingCity": "Los Angeles",
                "billingCountry": "US",
                "billingZip": "90001",
                "idempotencyKey": "idemp-002"
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest1));

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest2));

        List<Payment> allPayments = paymentRepository.findAll();
        assertEquals(2, allPayments.size());
        assertTrue(allPayments.stream().anyMatch(p -> p.getCardHolder().equals("User One")));
        assertTrue(allPayments.stream().anyMatch(p -> p.getCardHolder().equals("User Two")));
    }

    @Test
    void paymentStatus_ShouldBePersistedCorrectly() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "PAY-STATUS-001", "idemp-001", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        ));

        payment.setStatus(PaymentStatus.AUTHORIZED);
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getPaymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        Payment updatedPayment = paymentRepository.findByPaymentId(payment.getPaymentId()).orElseThrow();
        assertEquals(PaymentStatus.AUTHORIZED, updatedPayment.getStatus());
    }

    @Test
    void cardNumber_ShouldBeMaskedInResponse() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.50,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "customerId": "CUST-001",
                "description": "Test payment",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "billingAddress": "123 Main St",
                "billingCity": "New York",
                "billingCountry": "US",
                "billingZip": "10001",
                "idempotencyKey": "idemp-mask-001"
            }
            """;

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value(startsWith("****-****-****-")))
                .andExpect(jsonPath("$.cardNumber").value(endsWith("1111")));

        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertTrue(payments.get(0).getCardNumber().startsWith("****-****-****-"));
    }
}
