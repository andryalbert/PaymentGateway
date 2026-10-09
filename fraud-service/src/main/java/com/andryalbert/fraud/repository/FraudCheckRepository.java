package com.andryalbert.fraud.repository;

import com.andryalbert.fraud.entity.FraudCheck;
import com.andryalbert.fraud.entity.FraudStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {
    
    Optional<FraudCheck> findByCheckId(String checkId);
    
    Optional<FraudCheck> findByPaymentId(String paymentId);
    
    List<FraudCheck> findByCustomerId(String customerId);
    
    List<FraudCheck> findByStatus(FraudStatus status);
    
    @Query("SELECT COUNT(f) FROM FraudCheck f WHERE f.customerId = :customerId AND f.createdAt >= :since")
    Long countByCustomerIdAndCreatedAtAfter(@Param("customerId") String customerId, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(f.amount) FROM FraudCheck f WHERE f.customerId = :customerId AND f.status = 'APPROVED' AND f.createdAt >= :since")
    java.math.BigDecimal sumApprovedAmountByCustomerIdAndCreatedAtAfter(@Param("customerId") String customerId, @Param("since") LocalDateTime since);
}
