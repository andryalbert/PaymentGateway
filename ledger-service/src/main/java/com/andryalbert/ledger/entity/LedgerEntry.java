package com.andryalbert.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String entryId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String referenceType;

    @Column(nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private String counterpartyAccount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerEntry(String entryId, String paymentId, String accountNumber, EntryType entryType,
                      BigDecimal amount, String currency, BigDecimal balanceAfter, String description,
                      String referenceType, String referenceId, String counterpartyAccount) {
        this.entryId = entryId;
        this.paymentId = paymentId;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.counterpartyAccount = counterpartyAccount;
    }

    public static String generateEntryId() {
        return "LED-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
