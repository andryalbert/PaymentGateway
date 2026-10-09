package com.andryalbert.fraud.service;

import com.andryalbert.fraud.entity.FraudCheck;
import com.andryalbert.fraud.entity.FraudRule;
import com.andryalbert.fraud.entity.FraudStatus;
import com.andryalbert.fraud.repository.FraudCheckRepository;
import com.andryalbert.fraud.repository.FraudRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final FraudCheckRepository fraudCheckRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final ObjectMapper objectMapper;

    @Value("${fraud.detection.max-amount-per-day}")
    private BigDecimal maxAmountPerDay;

    @Value("${fraud.detection.max-transactions-per-hour}")
    private Integer maxTransactionsPerHour;

    @Value("${fraud.detection.max-amount-per-transaction}")
    private BigDecimal maxAmountPerTransaction;

    @Value("${fraud.detection.suspicious-countries}")
    private String suspiciousCountries;

    @Transactional
    public void processPaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String paymentId = root.get("paymentId").asText();
            String customerId = root.get("customerId").asText();
            String cardNumber = root.get("cardNumber").asText();
            BigDecimal amount = root.get("amount").decimalValue();
            String currency = root.get("currency").asText();
            String ipAddress = root.has("ipAddress") ? root.get("ipAddress").asText() : "";
            String userAgent = root.has("userAgent") ? root.get("userAgent").asText() : "";
            String billingCountry = root.has("billingCountry") ? root.get("billingCountry").asText() : "";

            String checkId = FraudCheck.generateCheckId();
            FraudCheck fraudCheck = new FraudCheck(
                checkId, paymentId, customerId, cardNumber, amount, currency,
                ipAddress, userAgent, billingCountry
            );

            // Run fraud detection rules
            FraudDetectionResult result = runFraudDetection(fraudCheck);
            
            fraudCheck.setRiskScore(result.getRiskScore());
            fraudCheck.setStatus(result.getStatus());
            fraudCheck.setReason(result.getReason());
            fraudCheck.setDetails(result.getDetails());
            
            fraudCheckRepository.save(fraudCheck);

            log.info("Fraud check completed for payment {}: {} (risk score: {})", 
                paymentId, result.getStatus(), result.getRiskScore());

        } catch (Exception e) {
            log.error("Error processing payment event for fraud detection", e);
        }
    }

    private FraudDetectionResult runFraudDetection(FraudCheck fraudCheck) {
        List<String> reasons = new ArrayList<>();
        int riskScore = 0;

        // Rule 1: Amount threshold check
        if (fraudCheck.getAmount().compareTo(maxAmountPerTransaction) > 0) {
            riskScore += 30;
            reasons.add("Amount exceeds maximum allowed per transaction");
        }

        // Rule 2: Velocity check - transactions per hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long recentTransactions = fraudCheckRepository.countByCustomerIdAndCreatedAtAfter(
            fraudCheck.getCustomerId(), oneHourAgo);
        
        if (recentTransactions >= maxTransactionsPerHour) {
            riskScore += 25;
            reasons.add("Too many transactions in the last hour");
        }

        // Rule 3: Daily amount check
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        BigDecimal dailyAmount = fraudCheckRepository.sumApprovedAmountByCustomerIdAndCreatedAtAfter(
            fraudCheck.getCustomerId(), oneDayAgo);
        
        if (dailyAmount != null && dailyAmount.add(fraudCheck.getAmount()).compareTo(maxAmountPerDay) > 0) {
            riskScore += 20;
            reasons.add("Daily amount limit would be exceeded");
        }

        // Rule 4: Suspicious country check
        if (suspiciousCountries.contains(fraudCheck.getBillingCountry())) {
            riskScore += 40;
            reasons.add("Billing country is flagged as suspicious");
        }

        // Rule 5: IP address geolocation check (simplified)
        if (isSuspiciousIP(fraudCheck.getIpAddress())) {
            riskScore += 35;
            reasons.add("IP address is from high-risk location");
        }

        // Rule 6: Device fingerprinting check (simplified)
        if (isNewDevice(fraudCheck.getCustomerId(), fraudCheck.getUserAgent())) {
            riskScore += 15;
            reasons.add("New device detected for customer");
        }

        // Determine final status
        FraudStatus status;
        if (riskScore >= 70) {
            status = FraudStatus.REJECTED;
        } else if (riskScore >= 40) {
            status = FraudStatus.MANUAL_REVIEW;
        } else {
            status = FraudStatus.APPROVED;
        }

        String details = String.join("; ", reasons);
        return new FraudDetectionResult(status, riskScore, details.isEmpty() ? "No suspicious activity detected" : details, details);
    }

    private boolean isSuspiciousIP(String ipAddress) {
        // Simplified IP check - in production, use proper IP geolocation service
        return ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.");
    }

    private boolean isNewDevice(String customerId, String userAgent) {
        // Simplified device check - in production, maintain device registry
        return userAgent.contains("Bot") || userAgent.contains("Crawler");
    }

    private static class FraudDetectionResult {
        private final FraudStatus status;
        private final int riskScore;
        private final String reason;
        private final String details;

        public FraudDetectionResult(FraudStatus status, int riskScore, String reason, String details) {
            this.status = status;
            this.riskScore = riskScore;
            this.reason = reason;
            this.details = details;
        }

        public FraudStatus getStatus() {
            return status;
        }

        public int getRiskScore() {
            return riskScore;
        }

        public String getReason() {
            return reason;
        }

        public String getDetails() {
            return details;
        }
    }
}
