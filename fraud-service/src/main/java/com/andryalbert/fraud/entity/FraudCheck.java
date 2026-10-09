package com.andryalbert.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String checkId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String billingCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudStatus status;

    @Column(nullable = false)
    private Integer riskScore;

    @Column(length = 1000)
    private String reason;

    @Column(length = 1000)
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FraudCheck(String checkId, String paymentId, String customerId, String cardNumber,
                      BigDecimal amount, String currency, String ipAddress, String userAgent,
                      String billingCountry) {
        this.checkId = checkId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.currency = currency;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.billingCountry = billingCountry;
        this.status = FraudStatus.PENDING;
        this.riskScore = 0;
    }

    public static String generateCheckId() {
        return "FC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
