package com.andryalbert.payment.service;

import com.andryalbert.payment.dto.PaymentRequest;
import com.andryalbert.payment.dto.PaymentResponse;
import com.andryalbert.payment.entity.Payment;
import com.andryalbert.payment.entity.PaymentStatus;
import com.andryalbert.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponse createPayment(PaymentRequest request) {
        // Idempotency check
        paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
            .ifPresent(existingPayment -> {
                throw new IllegalStateException("Payment already processed with idempotency key: " + request.getIdempotencyKey());
            });

        String paymentId = Payment.generatePaymentId();
        
        Payment payment = new Payment(
            paymentId,
            request.getIdempotencyKey(),
            request.getCardNumber(),
            request.getCardHolder(),
            request.getExpiryDate(),
            request.getCvv(),
            request.getAmount(),
            request.getCurrency(),
            request.getMerchantId(),
            request.getDescription(),
            request.getCustomerId(),
            request.getIpAddress(),
            request.getUserAgent(),
            request.getBillingAddress(),
            request.getBillingCity(),
            request.getBillingCountry(),
            request.getBillingZip()
        );

        payment.setStatus(PaymentStatus.VALIDATING);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created: {}", paymentId);
        return mapToResponse(savedPayment, "Payment created successfully");
    }

    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        return mapToResponse(payment, "Payment retrieved成功");
    }

    public List<PaymentResponse> getPaymentsByMerchant(String merchantId) {
        List<Payment> payments = paymentRepository.findByMerchantId(merchantId);
        return payments.stream()
            .map(payment -> mapToResponse(payment, "Payment retrieved successfully"))
            .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByCustomer(String customerId) {
        List<Payment> payments = paymentRepository.findByCustomerId(customerId);
        return payments.stream()
            .map(payment -> mapToResponse(payment, "Payment retrieved successfully"))
            .collect(Collectors.toList());
    }

    public PaymentResponse refundPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SETTLED && payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Only settled or captured payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment refunded: {}", paymentId);
        return mapToResponse(savedPayment, "Payment refunded successfully");
    }

    public PaymentResponse cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SETTLED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel settled or refunded payment");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled: {}", paymentId);
        return mapToResponse(savedPayment, "Payment cancelled successfully");
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        return new PaymentResponse(
            payment.getId(),
            payment.getPaymentId(),
            payment.getIdempotencyKey(),
            payment.getCardNumber(),
            payment.getCardHolder(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getMerchantId(),
            payment.getCustomerId(),
            payment.getDescription(),
            payment.getCreatedAt(),
            payment.getUpdatedAt(),
            message
        );
    }
}
