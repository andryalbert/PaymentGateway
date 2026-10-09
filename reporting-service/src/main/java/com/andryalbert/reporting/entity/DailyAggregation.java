package com.andryalbert.reporting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_aggregations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAggregation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private LocalDate aggregationDate;

    @Column(nullable = false)
    private Integer totalTransactions;

    @Column(nullable = false)
    private Integer successfulTransactions;

    @Column(nullable = false)
    private Integer failedTransactions;

    @Column(nullable = false)
    private Integer fraudRejectedTransactions;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal successfulAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal failedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal averageTransactionValue;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFees;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
