package com.andryalbert.settlement.repository;

import com.andryalbert.settlement.entity.SettlementItem;
import com.andryalbert.settlement.entity.SettlementItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {
    
    Optional<SettlementItem> findByItemId(String itemId);
    
    List<SettlementItem> findBySettlementId(String settlementId);
    
    List<SettlementItem> findByPaymentId(String paymentId);
    
    List<SettlementItem> findByStatus(SettlementItemStatus status);
}
