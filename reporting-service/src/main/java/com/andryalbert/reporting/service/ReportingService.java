package com.andryalbert.reporting.service;

import com.andryalbert.reporting.entity.DailyAggregation;
import com.andryalbert.reporting.entity.PaymentReport;
import com.andryalbert.reporting.repository.DailyAggregationRepository;
import com.andryalbert.reporting.repository.PaymentReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final PaymentReportRepository paymentReportRepository;
    private final DailyAggregationRepository dailyAggregationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String paymentId = root.get("paymentId").asText();
            String merchantId = root.get("merchantId").asText();
            String customerId = root.get("customerId").asText();
            BigDecimal amount = new BigDecimal(root.get("amount").asText()).setScale(2, RoundingMode.HALF_UP);
            String currency = root.get("currency").asText();
            String status = root.has("status") ? root.get("status").asText() : "";
            LocalDateTime eventTimestamp = root.has("timestamp") ? 
                LocalDateTime.parse(root.get("timestamp").asText()) : LocalDateTime.now();

            // Create payment report
            PaymentReport report = new PaymentReport();
            report.setPaymentId(paymentId);
            report.setMerchantId(merchantId);
            report.setCustomerId(customerId);
            report.setAmount(amount);
            report.setCurrency(currency);
            report.setStatus(status);
            report.setEventType(eventType);
            report.setEventTimestamp(eventTimestamp);
            report.setFraudCheckResult(status.equals("REJECTED") ? "FRAUD_DETECTED" : "CLEARED");
            report.setFraudRiskScore(status.equals("REJECTED") ? 100 : 0);

            paymentReportRepository.save(report);
            log.info("Payment report created for: {}", paymentId);

        } catch (Exception e) {
            log.error("Error processing payment event for reporting", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    @Transactional
    public void generateDailyAggregations() {
        log.info("Starting daily aggregation generation");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

        // Get all unique merchants from payment reports
        // In production, you might want to maintain a merchant registry
        List<String> merchantIds = paymentReportRepository.findAll().stream()
            .map(PaymentReport::getMerchantId)
            .distinct()
            .toList();

        for (String merchantId : merchantIds) {
            generateDailyAggregationForMerchant(merchantId, yesterday, startOfDay, endOfDay);
        }

        log.info("Daily aggregation generation completed");
    }

    @Transactional
    public void generateDailyAggregationForMerchant(String merchantId, LocalDate date, 
                                                   LocalDateTime startOfDay, LocalDateTime endOfDay) {
        // Check if aggregation already exists
        if (dailyAggregationRepository.findByMerchantIdAndAggregationDate(merchantId, date).isPresent()) {
            log.info("Aggregation already exists for merchant {} on date {}", merchantId, date);
            return;
        }

        // Calculate metrics
        Long totalTransactions = paymentReportRepository.countByMerchantIdAndEventTimestampAfter(merchantId, startOfDay);
        BigDecimal totalAmount = paymentReportRepository.sumSuccessfulAmountByMerchantIdAndEventTimestampAfter(merchantId, startOfDay);
        Long fraudRejectedCount = paymentReportRepository.countFraudRejectedByMerchantIdAndEventTimestampAfter(merchantId, startOfDay);

        if (totalTransactions == null) totalTransactions = 0L;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (fraudRejectedCount == null) fraudRejectedCount = 0L;

        // Calculate derived metrics
        Integer successfulTransactions = totalTransactions.intValue() - fraudRejectedCount.intValue();
        Integer failedTransactions = fraudRejectedCount.intValue();
        BigDecimal successfulAmount = totalAmount;
        BigDecimal failedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); // In production, calculate from failed transactions
        BigDecimal averageTransactionValue = totalTransactions > 0 ? 
            totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal totalFees = totalAmount.multiply(new BigDecimal("0.029")); // 2.9% fee

        // Create daily aggregation
        DailyAggregation aggregation = new DailyAggregation();
        aggregation.setMerchantId(merchantId);
        aggregation.setAggregationDate(date);
        aggregation.setTotalTransactions(totalTransactions.intValue());
        aggregation.setSuccessfulTransactions(successfulTransactions);
        aggregation.setFailedTransactions(failedTransactions);
        aggregation.setFraudRejectedTransactions(fraudRejectedCount.intValue());
        aggregation.setTotalAmount(totalAmount);
        aggregation.setSuccessfulAmount(successfulAmount);
        aggregation.setFailedAmount(failedAmount);
        aggregation.setAverageTransactionValue(averageTransactionValue);
        aggregation.setTotalFees(totalFees);

        dailyAggregationRepository.save(aggregation);
        log.info("Daily aggregation created for merchant {} on date {}", merchantId, date);
    }

    public List<PaymentReport> getMerchantReports(String merchantId) {
        return paymentReportRepository.findByMerchantId(merchantId);
    }

    public List<DailyAggregation> getMerchantAggregations(String merchantId) {
        return dailyAggregationRepository.findByMerchantIdOrderByAggregationDateDesc(merchantId);
    }

    public List<DailyAggregation> getAllAggregationsForDate(LocalDate date) {
        return dailyAggregationRepository.findByAggregationDateOrderByAggregationDateDesc(date);
    }
}
