package com.andryalbert.ledger.repository;

import com.andryalbert.ledger.entity.LedgerEntry;
import com.andryalbert.ledger.entity.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    Optional<LedgerEntry> findByEntryId(String entryId);
    
    List<LedgerEntry> findByPaymentId(String paymentId);
    
    List<LedgerEntry> findByAccountNumber(String accountNumber);
    
    List<LedgerEntry> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    
    List<LedgerEntry> findByEntryType(EntryType entryType);
}
