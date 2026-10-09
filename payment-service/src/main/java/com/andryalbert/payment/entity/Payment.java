package com.andryalbert.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String cardHolder;

    @Column(nullable = false)
    private String expiryDate;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String merchantId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String billingAddress;

    @Column(nullable = false)
    private String billingCity;

    @Column(nullable = false)
    private String billingCountry;

    @Column(nullable = false)
    private String billingZip;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Payment(String paymentId, String idempotencyKey, String cardNumber, String cardHolder,
                   String expiryDate, String cvv, BigDecimal amount, String currency,
                   String merchantId, String description, String customerId, String ipAddress,
                   String userAgent, String billingAddress, String billingCity,
                   String billingCountry, String billingZip) {
        this.paymentId = paymentId;
        this.idempotencyKey = idempotencyKey;
        this.cardNumber = maskCardNumber(cardNumber);
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.merchantId = merchantId;
        this.description = description;
        this.customerId = customerId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.billingAddress = billingAddress;
        this.billingCity = billingCity;
        this.billingCountry = billingCountry;
        this.billingZip = billingZip;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    public static String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
