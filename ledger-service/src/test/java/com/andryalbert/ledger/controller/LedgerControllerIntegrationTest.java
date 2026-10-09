package com.andryalbert.ledger.controller;

import com.andryalbert.ledger.entity.Account;
import com.andryalbert.ledger.entity.EntryType;
import com.andryalbert.ledger.entity.LedgerEntry;
import com.andryalbert.ledger.repository.AccountRepository;
import com.andryalbert.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LedgerControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("ledgerdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_ShouldReturnCreatedAccount() throws Exception {
        mockMvc.perform(post("/api/v1/ledger/accounts")
                .param("accountHolderName", "John Doe")
                .param("accountType", "CUSTOMER")
                .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.accountType").value("CUSTOMER"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountNumber").isString())
                .andExpect(jsonPath("$.accountNumber").value(startsWith("ACC-")));

        List<Account> accounts = accountRepository.findAll();
        assertEquals(1, accounts.size());
        assertEquals("John Doe", accounts.get(0).getAccountHolderName());
    }

    @Test
    void getAccount_ShouldReturnAccount_WhenAccountExists() throws Exception {
        Account account = accountRepository.save(new Account(
            "ACC-TEST-001", "CUSTOMER", "Jane Smith", "EUR",
            new BigDecimal("500.00"), "ACTIVE"));

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountNumber}", account.getAccountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(account.getAccountNumber()))
                .andExpect(jsonPath("$.accountHolderName").value("Jane Smith"))
                .andExpect(jsonPath("$.accountType").value("CUSTOMER"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccount_ShouldReturn404_WhenAccountNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/ledger/accounts/{accountNumber}", "ACC-NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountHistory_ShouldReturnEmptyList_WhenNoEntries() throws Exception {
        Account account = accountRepository.save(new Account(
            "ACC-TEST-002", "MERCHANT", "Merchant Inc", "USD",
            new BigDecimal("10000.00"), "ACTIVE"));

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountNumber}/history", account.getAccountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAccountHistory_ShouldReturnEntries_WhenEntriesExist() throws Exception {
        Account account = accountRepository.save(new Account(
            "ACC-TEST-003", "CUSTOMER", "Bob Johnson", "USD",
            new BigDecimal("1000.00"), "ACTIVE"));

        LedgerEntry entry1 = ledgerEntryRepository.save(new LedgerEntry(
            "LED-001", "PAY-001", account.getAccountNumber(), EntryType.CREDIT,
            new BigDecimal("100.00"), "USD", new BigDecimal("1100.00"),
            "Test credit", "PAYMENT", "PAY-001", "ACC-MERCHANT"));

        LedgerEntry entry2 = ledgerEntryRepository.save(new LedgerEntry(
            "LED-002", "PAY-002", account.getAccountNumber(), EntryType.DEBIT,
            new BigDecimal("50.00"), "USD", new BigDecimal("1050.00"),
            "Test debit", "PAYMENT", "PAY-002", "ACC-MERCHANT"));

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountNumber}/history", account.getAccountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].accountNumber", everyItem(is(account.getAccountNumber()))))
                .andExpect(jsonPath("$[*].entryId", containsInAnyOrder("LED-001", "LED-002")));
    }

    @Test
    void getPaymentEntries_ShouldReturnEmptyList_WhenNoEntries() throws Exception {
        mockMvc.perform(get("/api/v1/ledger/payments/{paymentId}/entries", "PAY-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPaymentEntries_ShouldReturnEntries_WhenEntriesExist() throws Exception {
        Account account1 = accountRepository.save(new Account(
            "ACC-TEST-004", "CUSTOMER", "Customer A", "USD",
            new BigDecimal("1000.00"), "ACTIVE"));

        Account account2 = accountRepository.save(new Account(
            "ACC-TEST-005", "MERCHANT", "Merchant B", "USD",
            new BigDecimal("5000.00"), "ACTIVE"));

        LedgerEntry entry1 = ledgerEntryRepository.save(new LedgerEntry(
            "LED-003", "PAY-100", account1.getAccountNumber(), EntryType.DEBIT,
            new BigDecimal("200.00"), "USD", new BigDecimal("800.00"),
            "Payment debit", "PAYMENT", "PAY-100", account2.getAccountNumber()));

        LedgerEntry entry2 = ledgerEntryRepository.save(new LedgerEntry(
            "LED-004", "PAY-100", account2.getAccountNumber(), EntryType.HOLD,
            new BigDecimal("200.00"), "USD", new BigDecimal("5000.00"),
            "Payment hold", "PAYMENT", "PAY-100", account1.getAccountNumber()));

        mockMvc.perform(get("/api/v1/ledger/payments/{paymentId}/entries", "PAY-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].paymentId", everyItem(is("PAY-100"))))
                .andExpect(jsonPath("$[*].entryId", containsInAnyOrder("LED-003", "LED-004")));
    }

    @Test
    void health_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/ledger/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ledger Service is healthy"));
    }

    @Test
    void fullFlow_CreateAccountAndGetHistory() throws Exception {
        String accountNumber = mockMvc.perform(post("/api/v1/ledger/accounts")
                .param("accountHolderName", "Full Flow User")
                .param("accountType", "CUSTOMER")
                .param("currency", "USD"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        accountNumber = accountNumber.substring(accountNumber.indexOf("\"accountNumber\":\"") + 17, 
            accountNumber.indexOf("\"", accountNumber.indexOf("\"accountNumber\":\"") + 17));

        LedgerEntry entry = ledgerEntryRepository.save(new LedgerEntry(
            "LED-FLOW-001", "PAY-FLOW-001", accountNumber, EntryType.CREDIT,
            new BigDecimal("500.00"), "USD", new BigDecimal("500.00"),
            "Flow test credit", "PAYMENT", "PAY-FLOW-001", "ACC-MERCHANT"));

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountNumber}/history", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entryId").value("LED-FLOW-001"))
                .andExpect(jsonPath("$[0].amount").value(500.00));
    }

    @Test
    void multipleAccounts_ShouldBeStoredIndependently() throws Exception {
        mockMvc.perform(post("/api/v1/ledger/accounts")
                .param("accountHolderName", "User One")
                .param("accountType", "CUSTOMER")
                .param("currency", "USD"));

        mockMvc.perform(post("/api/v1/ledger/accounts")
                .param("accountHolderName", "User Two")
                .param("accountType", "MERCHANT")
                .param("currency", "EUR"));

        List<Account> accounts = accountRepository.findAll();
        assertEquals(2, accounts.size());
        assertTrue(accounts.stream().anyMatch(a -> a.getAccountHolderName().equals("User One")));
        assertTrue(accounts.stream().anyMatch(a -> a.getAccountHolderName().equals("User Two")));
    }
}
