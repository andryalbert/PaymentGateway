package com.andryalbert.ledger.service;

import com.andryalbert.ledger.entity.Account;
import com.andryalbert.ledger.entity.EntryType;
import com.andryalbert.ledger.entity.LedgerEntry;
import com.andryalbert.ledger.repository.AccountRepository;
import com.andryalbert.ledger.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LedgerService ledgerService;

    private Account testAccount;
    private LedgerEntry testEntry;

    @BeforeEach
    void setUp() {
        testAccount = new Account("ACC-123", "CUSTOMER", "John Doe", "USD", 
            new BigDecimal("1000.00"), "ACTIVE");
        testEntry = new LedgerEntry("LED-001", "PAY-001", "ACC-123", EntryType.CREDIT,
            new BigDecimal("100.00"), "USD", new BigDecimal("1100.00"), "Test credit",
            "PAYMENT", "PAY-001", "ACC-456");
    }

    @Test
    void createAccount_ShouldReturnCreatedAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = ledgerService.createAccount("John Doe", "CUSTOMER", "USD");

        assertNotNull(result);
        assertEquals("John Doe", result.getAccountHolderName());
        assertEquals("CUSTOMER", result.getAccountType());
        assertEquals("USD", result.getCurrency());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void getAccount_ShouldReturnAccount_WhenAccountExists() {
        when(accountRepository.findByAccountNumber("ACC-123")).thenReturn(Optional.of(testAccount));

        Account result = ledgerService.getAccount("ACC-123");

        assertNotNull(result);
        assertEquals("ACC-123", result.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber("ACC-123");
    }

    @Test
    void getAccount_ShouldThrowException_WhenAccountNotFound() {
        when(accountRepository.findByAccountNumber("ACC-999")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ledgerService.getAccount("ACC-999");
        });

        assertTrue(exception.getMessage().contains("Account not found"));
        verify(accountRepository, times(1)).findByAccountNumber("ACC-999");
    }

    @Test
    void getAccountHistory_ShouldReturnEntries() {
        List<LedgerEntry> entries = Arrays.asList(testEntry);
        when(ledgerEntryRepository.findByAccountNumberOrderByCreatedAtDesc("ACC-123"))
            .thenReturn(entries);

        List<LedgerEntry> result = ledgerService.getAccountHistory("ACC-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LED-001", result.get(0).getEntryId());
        verify(ledgerEntryRepository, times(1))
            .findByAccountNumberOrderByCreatedAtDesc("ACC-123");
    }

    @Test
    void getPaymentEntries_ShouldReturnEntries() {
        List<LedgerEntry> entries = Arrays.asList(testEntry);
        when(ledgerEntryRepository.findByPaymentId("PAY-001")).thenReturn(entries);

        List<LedgerEntry> result = ledgerService.getPaymentEntries("PAY-001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PAY-001", result.get(0).getPaymentId());
        verify(ledgerEntryRepository, times(1)).findByPaymentId("PAY-001");
    }

    @Test
    void processPaymentEvent_PAYMENT_CREATED_ShouldCreateHoldEntry() throws Exception {
        String message = "{\"eventType\":\"PAYMENT_CREATED\",\"paymentId\":\"PAY-001\"," +
            "\"customerId\":\"CUST-001\",\"amount\":100.00,\"currency\":\"USD\",\"merchantId\":\"MERC-001\"}";
        
        com.fasterxml.jackson.databind.JsonNode root = mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapper.readTree(message)).thenReturn(root);
        when(root.get("eventType")).thenReturn(root);
        when(root.get("paymentId")).thenReturn(root);
        when(root.get("customerId")).thenReturn(root);
        when(root.get("amount")).thenReturn(root);
        when(root.get("currency")).thenReturn(root);
        when(root.get("merchantId")).thenReturn(root);
        when(root.asText()).thenReturn("PAYMENT_CREATED", "PAY-001", "CUST-001", "USD", "MERC-001", "100.00");

        Account customerAccount = new Account("ACC-CUST", "CUSTOMER", "CUST-001", "USD",
            new BigDecimal("1000.00"), "ACTIVE");
        Account merchantAccount = new Account("ACC-MERC", "MERCHANT", "MERC-001", "USD",
            new BigDecimal("5000.00"), "ACTIVE");

        when(accountRepository.findByAccountHolderName("CUST-001")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(customerAccount, merchantAccount);

        ledgerService.processPaymentEvent(message);

        verify(accountRepository, atLeast(2)).save(any(Account.class));
        verify(ledgerEntryRepository, atLeastOnce()).save(any(LedgerEntry.class));
    }

    @Test
    void processPaymentEvent_PAYMENT_AUTHORIZED_ShouldCreateDebitAndHoldEntries() throws Exception {
        String message = "{\"eventType\":\"PAYMENT_AUTHORIZED\",\"paymentId\":\"PAY-001\"," +
            "\"customerId\":\"CUST-001\",\"amount\":100.00,\"currency\":\"USD\",\"merchantId\":\"MERC-001\"}";
        
        com.fasterxml.jackson.databind.JsonNode root = mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapper.readTree(message)).thenReturn(root);
        when(root.get("eventType")).thenReturn(root);
        when(root.get("paymentId")).thenReturn(root);
        when(root.get("customerId")).thenReturn(root);
        when(root.get("amount")).thenReturn(root);
        when(root.get("currency")).thenReturn(root);
        when(root.get("merchantId")).thenReturn(root);
        when(root.asText()).thenReturn("PAYMENT_AUTHORIZED", "PAY-001", "CUST-001", "USD", "MERC-001", "100.00");

        Account customerAccount = new Account("ACC-CUST", "CUSTOMER", "CUST-001", "USD",
            new BigDecimal("1000.00"), "ACTIVE");
        Account merchantAccount = new Account("ACC-MERC", "MERCHANT", "MERC-001", "USD",
            new BigDecimal("5000.00"), "ACTIVE");

        when(accountRepository.findByAccountHolderName("CUST-001")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(customerAccount, merchantAccount);

        ledgerService.processPaymentEvent(message);

        verify(accountRepository, atLeast(2)).save(any(Account.class));
        verify(ledgerEntryRepository, atLeast(2)).save(any(LedgerEntry.class));
    }

    @Test
    void processPaymentEvent_UnknownEventType_ShouldLogWarning() throws Exception {
        String message = "{\"eventType\":\"UNKNOWN\",\"paymentId\":\"PAY-001\"," +
            "\"customerId\":\"CUST-001\",\"amount\":100.00,\"currency\":\"USD\",\"merchantId\":\"MERC-001\"}";
        
        com.fasterxml.jackson.databind.JsonNode root = mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapper.readTree(message)).thenReturn(root);
        when(root.get("eventType")).thenReturn(root);
        when(root.get("paymentId")).thenReturn(root);
        when(root.get("customerId")).thenReturn(root);
        when(root.get("amount")).thenReturn(root);
        when(root.get("currency")).thenReturn(root);
        when(root.get("merchantId")).thenReturn(root);
        when(root.asText()).thenReturn("UNKNOWN", "PAY-001", "CUST-001", "USD", "MERC-001", "100.00");

        ledgerService.processPaymentEvent(message);

        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void processPaymentEvent_InvalidJson_ShouldLogError() throws Exception {
        String message = "invalid json";
        when(objectMapper.readTree(message)).thenThrow(new RuntimeException("Invalid JSON"));

        ledgerService.processPaymentEvent(message);

        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }
}
