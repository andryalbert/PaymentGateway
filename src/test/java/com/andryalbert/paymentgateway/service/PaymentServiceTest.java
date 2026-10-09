package com.andryalbert.paymentgateway.service;

import com.andryalbert.paymentgateway.dto.PaymentRequest;
import com.andryalbert.paymentgateway.dto.PaymentResponse;
import com.andryalbert.paymentgateway.entity.Payment;
import com.andryalbert.paymentgateway.entity.PaymentStatus;
import com.andryalbert.paymentgateway.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setCardNumber("4111111111111111");
        paymentRequest.setCardHolder("John Doe");
        paymentRequest.setExpiryDate("12/25");
        paymentRequest.setCvv("123");
        paymentRequest.setAmount(new BigDecimal("100.50"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setMerchantId("MERCHANT-001");
        paymentRequest.setDescription("Test payment");

        payment = new Payment(
            "TXN-001", "****-****-****-1111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment"
        );
        payment.setId(1L);
    }

    @Test
    void processPayment_ShouldSavePaymentWithCompletedStatus_WhenAmountLessThan10000() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals("Payment processed successfully", response.getMessage());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_ShouldSavePaymentWithFailedStatus_WhenAmountGreaterThan10000() {
        paymentRequest.setAmount(new BigDecimal("15000.00"));
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("Payment processing failed", response.getMessage());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_ShouldMaskCardNumber() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertTrue(response.getCardNumber().startsWith("****-****-****-"));
        assertTrue(response.getCardNumber().endsWith("1111"));
    }

    @Test
    void getPaymentByTransactionId_ShouldReturnPayment_WhenPaymentExists() {
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByTransactionId("TXN-001");

        assertNotNull(response);
        assertEquals("TXN-001", response.getTransactionId());
        assertEquals("John Doe", response.getCardHolder());

        verify(paymentRepository).findByTransactionId("TXN-001");
    }

    @Test
    void getPaymentByTransactionId_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findByTransactionId("TXN-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByTransactionId("TXN-NONEXISTENT"));

        verify(paymentRepository).findByTransactionId("TXN-NONEXISTENT");
    }

    @Test
    void getPaymentsByMerchantId_ShouldReturnPayments_WhenPaymentsExist() {
        Payment payment2 = new Payment(
            "TXN-002", "****-****-****-2222", "Jane Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-001", "Test payment 2"
        );

        when(paymentRepository.findByMerchantId("MERCHANT-001"))
            .thenReturn(Arrays.asList(payment, payment2));

        List<PaymentResponse> responses = paymentService.getPaymentsByMerchantId("MERCHANT-001");

        assertEquals(2, responses.size());
        assertEquals("TXN-001", responses.get(0).getTransactionId());
        assertEquals("TXN-002", responses.get(1).getTransactionId());

        verify(paymentRepository).findByMerchantId("MERCHANT-001");
    }

    @Test
    void getPaymentsByMerchantId_ShouldReturnEmptyList_WhenNoPayments() {
        when(paymentRepository.findByMerchantId("MERCHANT-NONEXISTENT"))
            .thenReturn(Arrays.asList());

        List<PaymentResponse> responses = paymentService.getPaymentsByMerchantId("MERCHANT-NONEXISTENT");

        assertTrue(responses.isEmpty());

        verify(paymentRepository).findByMerchantId("MERCHANT-NONEXISTENT");
    }

    @Test
    void refundPayment_ShouldRefundPayment_WhenPaymentIsCompleted() {
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment("TXN-001");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals("Payment refunded successfully", response.getMessage());

        verify(paymentRepository).findByTransactionId("TXN-001");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentIsNotCompleted() {
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class, () -> paymentService.refundPayment("TXN-001"));

        verify(paymentRepository).findByTransactionId("TXN-001");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findByTransactionId("TXN-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.refundPayment("TXN-NONEXISTENT"));

        verify(paymentRepository).findByTransactionId("TXN-NONEXISTENT");
    }
}
