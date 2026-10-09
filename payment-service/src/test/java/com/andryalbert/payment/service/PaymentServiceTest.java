package com.andryalbert.payment.service;

import com.andryalbert.payment.dto.PaymentRequest;
import com.andryalbert.payment.dto.PaymentResponse;
import com.andryalbert.payment.entity.Payment;
import com.andryalbert.payment.entity.PaymentStatus;
import com.andryalbert.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        paymentRequest.setCustomerId("CUST-001");
        paymentRequest.setDescription("Test payment");
        paymentRequest.setIpAddress("192.168.1.1");
        paymentRequest.setUserAgent("Mozilla/5.0");
        paymentRequest.setBillingAddress("123 Main St");
        paymentRequest.setBillingCity("New York");
        paymentRequest.setBillingCountry("US");
        paymentRequest.setBillingZip("10001");
        paymentRequest.setIdempotencyKey("idemp-123");

        payment = new Payment(
            "PAY-001", "idemp-123", "4111111111111111", "John Doe",
            "12/25", "123", new BigDecimal("100.50"), "USD",
            "MERCHANT-001", "Test payment", "CUST-001", "192.168.1.1",
            "Mozilla/5.0", "123 Main St", "New York", "US", "10001"
        );
        payment.setId(1L);
    }

    @Test
    void createPayment_ShouldSavePayment() {
        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment savedPayment = invocation.getArgument(0);
            savedPayment.setStatus(PaymentStatus.VALIDATING);
            return savedPayment;
        });

        PaymentResponse response = paymentService.createPayment(paymentRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.VALIDATING, response.getStatus());

        verify(paymentRepository).findByIdempotencyKey("idemp-123");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_ShouldThrowException_WhenIdempotencyKeyExists() {
        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.createPayment(paymentRequest));

        verify(paymentRepository).findByIdempotencyKey("idemp-123");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getPayment_ShouldReturnPayment_WhenPaymentExists() {
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment("PAY-001");

        assertNotNull(response);
        assertEquals("PAY-001", response.getPaymentId());
        assertEquals("John Doe", response.getCardHolder());

        verify(paymentRepository).findByPaymentId("PAY-001");
    }

    @Test
    void getPayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findByPaymentId("PAY-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPayment("PAY-NONEXISTENT"));

        verify(paymentRepository).findByPaymentId("PAY-NONEXISTENT");
    }

    @Test
    void getPaymentsByMerchant_ShouldReturnPayments_WhenPaymentsExist() {
        Payment payment2 = new Payment(
            "PAY-002", "idemp-456", "4111111111111111", "Jane Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-001", "Test payment 2", "CUST-002", "192.168.1.2",
            "Mozilla/5.0", "456 Oak St", "Los Angeles", "US", "90001"
        );

        when(paymentRepository.findByMerchantId("MERCHANT-001"))
            .thenReturn(Arrays.asList(payment, payment2));

        List<PaymentResponse> responses = paymentService.getPaymentsByMerchant("MERCHANT-001");

        assertEquals(2, responses.size());
        assertEquals("PAY-001", responses.get(0).getPaymentId());
        assertEquals("PAY-002", responses.get(1).getPaymentId());

        verify(paymentRepository).findByMerchantId("MERCHANT-001");
    }

    @Test
    void getPaymentsByMerchant_ShouldReturnEmptyList_WhenNoPayments() {
        when(paymentRepository.findByMerchantId("MERCHANT-NONEXISTENT"))
            .thenReturn(Arrays.asList());

        List<PaymentResponse> responses = paymentService.getPaymentsByMerchant("MERCHANT-NONEXISTENT");

        assertTrue(responses.isEmpty());

        verify(paymentRepository).findByMerchantId("MERCHANT-NONEXISTENT");
    }

    @Test
    void getPaymentsByCustomer_ShouldReturnPayments_WhenPaymentsExist() {
        Payment payment2 = new Payment(
            "PAY-002", "idemp-456", "4111111111111111", "Jane Doe",
            "12/25", "123", new BigDecimal("200.00"), "USD",
            "MERCHANT-002", "Test payment 2", "CUST-001", "192.168.1.2",
            "Mozilla/5.0", "456 Oak St", "Los Angeles", "US", "90001"
        );

        when(paymentRepository.findByCustomerId("CUST-001"))
            .thenReturn(Arrays.asList(payment, payment2));

        List<PaymentResponse> responses = paymentService.getPaymentsByCustomer("CUST-001");

        assertEquals(2, responses.size());
        assertEquals("PAY-001", responses.get(0).getPaymentId());
        assertEquals("PAY-002", responses.get(1).getPaymentId());

        verify(paymentRepository).findByCustomerId("CUST-001");
    }

    @Test
    void getPaymentsByCustomer_ShouldReturnEmptyList_WhenNoPayments() {
        when(paymentRepository.findByCustomerId("CUST-NONEXISTENT"))
            .thenReturn(Arrays.asList());

        List<PaymentResponse> responses = paymentService.getPaymentsByCustomer("CUST-NONEXISTENT");

        assertTrue(responses.isEmpty());

        verify(paymentRepository).findByCustomerId("CUST-NONEXISTENT");
    }

    @Test
    void refundPayment_ShouldRefundPayment_WhenPaymentIsSettled() {
        payment.setStatus(PaymentStatus.SETTLED);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment("PAY-001");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldRefundPayment_WhenPaymentIsCaptured() {
        payment.setStatus(PaymentStatus.CAPTURED);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment("PAY-001");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentIsNotSettledOrCaptured() {
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.refundPayment("PAY-001"));

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findByPaymentId("PAY-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.refundPayment("PAY-NONEXISTENT"));

        verify(paymentRepository).findByPaymentId("PAY-NONEXISTENT");
    }

    @Test
    void cancelPayment_ShouldCancelPayment_WhenPaymentIsNotSettledOrRefunded() {
        payment.setStatus(PaymentStatus.VALIDATING);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.cancelPayment("PAY-001");

        assertNotNull(response);
        assertEquals(PaymentStatus.CANCELLED, response.getStatus());

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void cancelPayment_ShouldThrowException_WhenPaymentIsSettled() {
        payment.setStatus(PaymentStatus.SETTLED);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.cancelPayment("PAY-001"));

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelPayment_ShouldThrowException_WhenPaymentIsRefunded() {
        payment.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.cancelPayment("PAY-001"));

        verify(paymentRepository).findByPaymentId("PAY-001");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelPayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findByPaymentId("PAY-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.cancelPayment("PAY-NONEXISTENT"));

        verify(paymentRepository).findByPaymentId("PAY-NONEXISTENT");
    }

    @Test
    void createPayment_ShouldMaskCardNumber() {
        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(paymentRequest);

        assertNotNull(response);
        assertTrue(response.getCardNumber().startsWith("****-****-****-"));
        assertTrue(response.getCardNumber().endsWith("1111"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertTrue(savedPayment.getCardNumber().startsWith("****-****-****-"));
    }
}
