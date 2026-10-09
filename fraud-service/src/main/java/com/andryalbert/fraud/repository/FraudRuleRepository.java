package com.andryalbert.fraud.repository;

import com.andryalbert.fraud.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {
    
    List<FraudRule> findByEnabledTrueOrderByPriorityAsc();
    
    List<FraudRule> findByRuleType(String ruleType);
}
