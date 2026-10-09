package com.andryalbert.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Account(String accountNumber, String accountType, String accountHolderName, 
                   String currency, BigDecimal balance, String status) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountHolderName = accountHolderName;
        this.currency = currency;
        this.balance = balance;
        this.availableBalance = balance;
        this.status = status;
    }

    public static String generateAccountNumber() {
        return "ACC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
