package com.andryalbert.paymentgateway.controller;

import com.andryalbert.paymentgateway.entity.Payment;
import com.andryalbert.paymentgateway.entity.PaymentStatus;
import com.andryalbert.paymentgateway.repository.PaymentRepository;
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
        .withDatabaseName("paymentgatewaydb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
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
    void processPayment_ShouldReturnCreatedPayment_WhenAmountLessThan10000() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.50,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "description": "Test payment"
            }
            """;

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isString())
                .andExpect(jsonPath("$.transactionId").startsWith("TXN-"))
                .andExpect(jsonPath("$.cardHolder").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.merchantId").value("MERCHANT-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cardNumber").startsWith("****-****-****-"))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"));

        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertEquals(PaymentStatus.COMPLETED, payments.get(0).getStatus());
    }

    @Test
    void processPayment_ShouldReturnFailedPayment_WhenAmountGreaterThan10000() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 15000.00,
                "currency": "USD",
                "merchantId": "MERCHANT-001",
                "description": "Test payment"
            }
            """;

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isString())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Payment processing failed"));

        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertEquals(PaymentStatus.FAILED, payments.get(0).getStatus());
    }

    @Test
    void getPaymentByTransactionId_ShouldReturnPayment_WhenPaymentExists() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "TXN-TEST-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment"
        ));

        mockMvc.perform(get("/api/payments/transaction/{transactionId}", payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(payment.getTransactionId()))
                .andExpect(jsonPath("$.cardHolder").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.merchantId").value("MERCHANT-001"));

        Payment retrievedPayment = paymentRepository.findByTransactionId(payment.getTransactionId()).orElseThrow();
        assertEquals("TXN-TEST-001", retrievedPayment.getTransactionId());
    }

    @Test
    void getPaymentByTransactionId_ShouldReturn500_WhenPaymentNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/transaction/{transactionId}", "TXN-NONEXISTENT"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getPaymentsByMerchantId_ShouldReturnPayments_WhenPaymentsExist() throws Exception {
        Payment payment1 = paymentRepository.save(new Payment(
            "TXN-TEST-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment 1"
        ));

        Payment payment2 = paymentRepository.save(new Payment(
            "TXN-TEST-002", "****-****-****-2222", "Jane Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-001", "Test payment 2"
        ));

        mockMvc.perform(get("/api/payments/merchant/{merchantId}", "MERCHANT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].merchantId", everyItem(is("MERCHANT-001"))))
                .andExpect(jsonPath("$[*].transactionId", containsInAnyOrder(
                    payment1.getTransactionId(),
                    payment2.getTransactionId()
                )));

        List<Payment> merchantPayments = paymentRepository.findByMerchantId("MERCHANT-001");
        assertEquals(2, merchantPayments.size());
    }

    @Test
    void getPaymentsByMerchantId_ShouldReturnEmptyList_WhenNoPayments() throws Exception {
        mockMvc.perform(get("/api/payments/merchant/{merchantId}", "MERCHANT-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void refundPayment_ShouldRefundPayment_WhenPaymentIsCompleted() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "TXN-TEST-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment"
        ));
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        mockMvc.perform(post("/api/payments/refund/{transactionId}", payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(payment.getTransactionId()))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.message").value("Payment refunded successfully"));

        Payment refundedPayment = paymentRepository.findByTransactionId(payment.getTransactionId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, refundedPayment.getStatus());
    }

    @Test
    void refundPayment_ShouldReturn500_WhenPaymentIsNotCompleted() throws Exception {
        Payment payment = paymentRepository.save(new Payment(
            "TXN-TEST-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment"
        ));

        mockMvc.perform(post("/api/payments/refund/{transactionId}", payment.getTransactionId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void refundPayment_ShouldReturn500_WhenPaymentNotFound() throws Exception {
        mockMvc.perform(post("/api/payments/refund/{transactionId}", "TXN-NONEXISTENT"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void health_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment Gateway API is running"));
    }

    @Test
    void fullFlow_ProcessAndRetrievePayment() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "Full Flow User",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 500.00,
                "currency": "USD",
                "merchantId": "MERCHANT-FLOW",
                "description": "Full flow test"
            }
            """;

        String responseContent = mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transactionId = objectMapper.readTree(responseContent).get("transactionId").asText();

        mockMvc.perform(get("/api/payments/transaction/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.cardHolder").value("Full Flow User"))
                .andExpect(jsonPath("$.amount").value(500.00));

        mockMvc.perform(get("/api/payments/merchant/{merchantId}", "MERCHANT-FLOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionId").value(transactionId));

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
                "description": "Payment 1"
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
                "description": "Payment 2"
            }
            """;

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest1));

        mockMvc.perform(post("/api/payments/process")
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
            "TXN-STATUS-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment"
        ));

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/transaction/{transactionId}", payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Payment updatedPayment = paymentRepository.findByTransactionId(payment.getTransactionId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, updatedPayment.getStatus());
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
                "description": "Test payment"
            }
            """;

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").startsWith("****-****-****-"))
                .andExpect(jsonPath("$.cardNumber").endsWith("1111"));

        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertTrue(payments.get(0).getCardNumber().startsWith("****-****-****-"));
    }

    @Test
    void refundFlow_ProcessThenRefundPayment() throws Exception {
        String paymentRequest = """
            {
                "cardNumber": "4111111111111111",
                "cardHolder": "Refund User",
                "expiryDate": "12/25",
                "cvv": "123",
                "amount": 100.00,
                "currency": "USD",
                "merchantId": "MERCHANT-REFUND",
                "description": "Refund test"
            }
            """;

        String responseContent = mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transactionId = objectMapper.readTree(responseContent).get("transactionId").asText();

        mockMvc.perform(post("/api/payments/refund/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.message").value("Payment refunded successfully"));

        Payment refundedPayment = paymentRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, refundedPayment.getStatus());
    }
}
