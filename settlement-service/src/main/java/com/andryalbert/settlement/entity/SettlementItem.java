package com.andryalbert.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String itemId;

    @Column(name = "settlement_id", insertable = false, updatable = false)
    private String settlementId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementItemStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "settlement_id", insertable = false, updatable = false)
    private Settlement settlement;

    public SettlementItem(String itemId, String paymentId,
                         BigDecimal amount, String currency, BigDecimal fee, BigDecimal netAmount) {
        this.itemId = itemId;
        // this.settlementId = settlementId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.fee = fee;
        this.netAmount = netAmount;
        this.status = SettlementItemStatus.PENDING;
    }

    public static String generateItemId() {
        return "STL-ITEM-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
