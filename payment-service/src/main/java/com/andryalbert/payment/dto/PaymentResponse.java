package com.andryalbert.payment.dto;

import com.andryalbert.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String paymentId;
    private String idempotencyKey;
    private String cardNumber;
    private String cardHolder;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String merchantId;
    private String customerId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
