package com.andryalbert.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "fraud_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String ruleType;

    @Column(nullable = false)
    private Boolean enabled;

    private BigDecimal thresholdAmount;
    private Integer thresholdCount;
    private Integer timeWindowMinutes;
    private String riskFactor;
    private Integer riskScore;
    private String description;

    @Column(nullable = false)
    private Integer priority;
}
