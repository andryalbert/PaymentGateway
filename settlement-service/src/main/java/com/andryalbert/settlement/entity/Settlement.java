package com.andryalbert.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String settlementId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(nullable = false)
    private String pspReference;

    @Column(length = 1000)
    private String pspResponse;

    @Column(nullable = false)
    private LocalDateTime settlementDate;

    @Column(nullable = false)
    private LocalDateTime expectedCompletionDate;

    @Column(nullable = false)
    private String bankAccount;

    @Column(nullable = false)
    private String routingNumber;

    @Column(length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL)
    private List<SettlementItem> items;

    public Settlement(String settlementId, String merchantId, String currency,
                      BigDecimal totalAmount, Integer transactionCount,
                      LocalDateTime settlementDate, LocalDateTime expectedCompletionDate,
                      String bankAccount, String routingNumber) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.status = SettlementStatus.PENDING;
        this.settlementDate = settlementDate;
        this.expectedCompletionDate = expectedCompletionDate;
        this.bankAccount = bankAccount;
        this.routingNumber = routingNumber;
    }

    public static String generateSettlementId() {
        return "STL-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
