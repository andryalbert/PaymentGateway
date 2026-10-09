package com.andryalbert.ledger.controller;

import com.andryalbert.ledger.entity.Account;
import com.andryalbert.ledger.entity.LedgerEntry;
import com.andryalbert.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestParam String accountHolderName,
                                                 @RequestParam String accountType,
                                                 @RequestParam String currency) {
        Account account = ledgerService.createAccount(accountHolderName, accountType, currency);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) {
        Account account = ledgerService.getAccount(accountNumber);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public ResponseEntity<List<LedgerEntry>> getAccountHistory(@PathVariable String accountNumber) {
        List<LedgerEntry> entries = ledgerService.getAccountHistory(accountNumber);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/payments/{paymentId}/entries")
    public ResponseEntity<List<LedgerEntry>> getPaymentEntries(@PathVariable String paymentId) {
        List<LedgerEntry> entries = ledgerService.getPaymentEntries(paymentId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ledger Service is healthy");
    }
}
