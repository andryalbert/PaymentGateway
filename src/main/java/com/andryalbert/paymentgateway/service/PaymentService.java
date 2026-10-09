package com.andryalbert.paymentgateway.service;

import com.andryalbert.paymentgateway.dto.PaymentRequest;
import com.andryalbert.paymentgateway.dto.PaymentResponse;
import com.andryalbert.paymentgateway.entity.Payment;
import com.andryalbert.paymentgateway.entity.PaymentStatus;
import com.andryalbert.paymentgateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        String transactionId = generateTransactionId();
        
        Payment payment = new Payment(
            transactionId,
            maskCardNumber(request.getCardNumber()),
            request.getCardHolder(),
            request.getExpiryDate(),
            request.getCvv(),
            request.getAmount(),
            request.getCurrency(),
            request.getMerchantId(),
            request.getDescription()
        );

        // Simulate payment processing
        boolean paymentSuccessful = simulatePaymentProcessing(request);
        
        if (paymentSuccessful) {
            payment.setStatus(PaymentStatus.COMPLETED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        Payment savedPayment = paymentRepository.save(payment);
        
        return mapToResponse(savedPayment, paymentSuccessful ? 
            "Payment processed successfully" : "Payment processing failed");
    }

    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found with transaction ID: " + transactionId));
        
        return mapToResponse(payment, "Payment retrieved successfully");
    }

    public List<PaymentResponse> getPaymentsByMerchantId(String merchantId) {
        List<Payment> payments = paymentRepository.findByMerchantId(merchantId);
        return payments.stream()
            .map(payment -> mapToResponse(payment, "Payment retrieved successfully"))
            .collect(Collectors.toList());
    }

    public PaymentResponse refundPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found with transaction ID: " + transactionId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);
        
        return mapToResponse(savedPayment, "Payment refunded successfully");
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    private boolean simulatePaymentProcessing(PaymentRequest request) {
        // Simulate payment processing logic
        // In a real implementation, this would integrate with a payment gateway
        return request.getAmount().compareTo(new BigDecimal("10000")) < 0;
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        return new PaymentResponse(
            payment.getId(),
            payment.getTransactionId(),
            payment.getCardNumber(),
            payment.getCardHolder(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getMerchantId(),
            payment.getDescription(),
            payment.getCreatedAt(),
            payment.getUpdatedAt(),
            message
        );
    }
}
