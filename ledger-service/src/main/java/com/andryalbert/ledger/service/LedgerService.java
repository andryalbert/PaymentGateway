package com.andryalbert.ledger.service;

import com.andryalbert.ledger.entity.Account;
import com.andryalbert.ledger.entity.EntryType;
import com.andryalbert.ledger.entity.LedgerEntry;
import com.andryalbert.ledger.repository.AccountRepository;
import com.andryalbert.ledger.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String paymentId = root.get("paymentId").asText();
            String customerId = root.get("customerId").asText();
            BigDecimal amount = new BigDecimal(root.get("amount").asText());
            String currency = root.get("currency").asText();
            String merchantId = root.get("merchantId").asText();

            // Get or create customer account
            Account customerAccount = getOrCreateAccount(customerId, "CUSTOMER", currency);
            
            // Get or create merchant account
            Account merchantAccount = getOrCreateAccount(merchantId, "MERCHANT", currency);

            switch (eventType) {
                case "PAYMENT_CREATED":
                    // Hold amount on customer account
                    createLedgerEntry(customerAccount, paymentId, EntryType.HOLD, amount, currency,
                        "Payment hold for " + paymentId, "PAYMENT", paymentId, merchantAccount.getAccountNumber());
                    break;

                case "PAYMENT_AUTHORIZED":
                    // Debit customer account
                    createLedgerEntry(customerAccount, paymentId, EntryType.DEBIT, amount, currency,
                        "Payment authorized for " + paymentId, "PAYMENT", paymentId, merchantAccount.getAccountNumber());
                    // Credit merchant account (pending settlement)
                    createLedgerEntry(merchantAccount, paymentId, EntryType.HOLD, amount, currency,
                        "Payment received pending settlement for " + paymentId, "PAYMENT", paymentId, customerAccount.getAccountNumber());
                    break;

                case "PAYMENT_REFUNDED":
                    // Credit customer account
                    createLedgerEntry(customerAccount, paymentId, EntryType.CREDIT, amount, currency,
                        "Refund for payment " + paymentId, "REFUND", paymentId, merchantAccount.getAccountNumber());
                    // Debit merchant account
                    createLedgerEntry(merchantAccount, paymentId, EntryType.DEBIT, amount, currency,
                        "Refund processed for payment " + paymentId, "REFUND", paymentId, customerAccount.getAccountNumber());
                    break;

                case "PAYMENT_CANCELLED":
                    // Release hold on customer account
                    createLedgerEntry(customerAccount, paymentId, EntryType.RELEASE, amount, currency,
                        "Payment cancelled - hold released for " + paymentId, "PAYMENT", paymentId, merchantAccount.getAccountNumber());
                    break;

                case "PAYMENT_SETTLED":
                    // Release merchant hold and credit available balance
                    createLedgerEntry(merchantAccount, paymentId, EntryType.RELEASE, amount, currency,
                        "Settlement completed for " + paymentId, "SETTLEMENT", paymentId, customerAccount.getAccountNumber());
                    createLedgerEntry(merchantAccount, paymentId, EntryType.CREDIT, amount, currency,
                        "Settlement credit for " + paymentId, "SETTLEMENT", paymentId, customerAccount.getAccountNumber());
                    break;

                default:
                    log.warn("Unknown payment event type: {}", eventType);
            }

            log.info("Ledger entries created for payment: {} with event: {}", paymentId, eventType);

        } catch (Exception e) {
            log.error("Error processing payment event for ledger", e);
        }
    }

    @Transactional
    public Account createAccount(String accountHolderName, String accountType, String currency) {
        String accountNumber = Account.generateAccountNumber();
        Account account = new Account(accountNumber, accountType, accountHolderName, currency, BigDecimal.ZERO, "ACTIVE");
        return accountRepository.save(account);
    }

    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    public List<LedgerEntry> getAccountHistory(String accountNumber) {
        return ledgerEntryRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<LedgerEntry> getPaymentEntries(String paymentId) {
        return ledgerEntryRepository.findByPaymentId(paymentId);
    }

    private Account getOrCreateAccount(String holderId, String accountType, String currency) {
        return accountRepository.findByAccountHolderName(holderId)
            .orElseGet(() -> createAccount(holderId, accountType, currency));
    }

    private void createLedgerEntry(Account account, String paymentId, EntryType entryType, 
                                  BigDecimal amount, String currency, String description, String referenceType,
                                  String referenceId, String counterpartyAccount) {
        BigDecimal balanceAfter = account.getBalance();
        
        switch (entryType) {
            case DEBIT:
            case FEE:
            case CHARGEBACK:
                balanceAfter = balanceAfter.subtract(amount);
                break;
            case CREDIT:
            case REFUND:
            case INTEREST:
                balanceAfter = balanceAfter.add(amount);
                break;
            case HOLD:
                // Hold doesn't change balance, only available balance
                account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
                break;
            case RELEASE:
                // Release hold, restore available balance
                account.setAvailableBalance(account.getAvailableBalance().add(amount));
                break;
        }

        if (entryType != EntryType.HOLD && entryType != EntryType.RELEASE) {
            account.setBalance(balanceAfter);
            account.setAvailableBalance(balanceAfter);
        }

        accountRepository.save(account);

        LedgerEntry entry = new LedgerEntry(
            LedgerEntry.generateEntryId(),
            paymentId,
            account.getAccountNumber(),
            entryType,
            amount,
            currency,
            balanceAfter,
            description,
            referenceType,
            referenceId,
            counterpartyAccount
        );

        ledgerEntryRepository.save(entry);
    }
}
