package com.andryalbert.reporting.repository;

import com.andryalbert.reporting.entity.DailyAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAggregationRepository extends JpaRepository<DailyAggregation, Long> {
    
    Optional<DailyAggregation> findByMerchantIdAndAggregationDate(String merchantId, LocalDate date);
    
    List<DailyAggregation> findByMerchantIdOrderByAggregationDateDesc(String merchantId);
    
    List<DailyAggregation> findByAggregationDateOrderByAggregationDateDesc(LocalDate date);
}
