package com.andryalbert.settlement.repository;

import com.andryalbert.settlement.entity.Settlement;
import com.andryalbert.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    
    Optional<Settlement> findBySettlementId(String settlementId);
    
    List<Settlement> findByMerchantId(String merchantId);
    
    List<Settlement> findByStatus(SettlementStatus status);
    
    List<Settlement> findByStatusAndSettlementDateBefore(SettlementStatus status, LocalDateTime date);
    
    List<Settlement> findByMerchantIdAndStatusOrderBySettlementDateDesc(String merchantId, SettlementStatus status);
    
    Optional<Settlement> findByPspReference(String pspReference);
}
