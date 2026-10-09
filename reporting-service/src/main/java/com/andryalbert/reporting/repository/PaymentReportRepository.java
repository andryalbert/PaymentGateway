package com.andryalbert.reporting.repository;

import com.andryalbert.reporting.entity.PaymentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentReportRepository extends JpaRepository<PaymentReport, Long> {
    
    List<PaymentReport> findByMerchantId(String merchantId);
    
    List<PaymentReport> findByCustomerId(String customerId);
    
    List<PaymentReport> findByMerchantIdAndEventTimestampBetween(
        String merchantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(p) FROM PaymentReport p WHERE p.merchantId = :merchantId AND p.eventTimestamp >= :since")
    Long countByMerchantIdAndEventTimestampAfter(@Param("merchantId") String merchantId, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(p.amount) FROM PaymentReport p WHERE p.merchantId = :merchantId AND p.status = 'AUTHORIZED' AND p.eventTimestamp >= :since")
    BigDecimal sumSuccessfulAmountByMerchantIdAndEventTimestampAfter(@Param("merchantId") String merchantId, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(p) FROM PaymentReport p WHERE p.merchantId = :merchantId AND p.status = 'REJECTED' AND p.eventTimestamp >= :since")
    Long countFraudRejectedByMerchantIdAndEventTimestampAfter(@Param("merchantId") String merchantId, @Param("since") LocalDateTime since);
}
