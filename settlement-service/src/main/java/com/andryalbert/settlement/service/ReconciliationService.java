package com.andryalbert.settlement.service;

import com.andryalbert.settlement.entity.Settlement;
import com.andryalbert.settlement.entity.SettlementItem;
import com.andryalbert.settlement.entity.SettlementItemStatus;
import com.andryalbert.settlement.entity.SettlementStatus;
import com.andryalbert.settlement.repository.SettlementItemRepository;
import com.andryalbert.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final WebClient webClient;

    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    @Transactional
    public void performDailyReconciliation() {
        log.info("Starting daily reconciliation process");
        
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = yesterday.toLocalDate().atTime(23, 59, 59);

        // Get completed settlements from yesterday
        List<Settlement> completedSettlements = settlementRepository.findByStatus(SettlementStatus.COMPLETED)
            .stream()
            .filter(s -> s.getSettlementDate().isAfter(startOfDay) && s.getSettlementDate().isBefore(endOfDay))
            .toList();

        log.info("Found {} completed settlements for reconciliation", completedSettlements.size());

        for (Settlement settlement : completedSettlements) {
            reconcileSettlement(settlement);
        }

        log.info("Daily reconciliation process completed");
    }

    @Transactional
    public void reconcileSettlement(Settlement settlement) {
        try {
            log.info("Reconciling settlement: {}", settlement.getSettlementId());

            // Get settlement items from internal ledger
            List<SettlementItem> internalItems = settlementItemRepository.findBySettlementId(settlement.getSettlementId());
            
            // Fetch external PSP report (simulated)
            Map<String, Object> pspReport = fetchPSPReport(settlement.getPspReference());
            
            // Compare internal vs external
            ReconciliationResult result = compareRecords(internalItems, pspReport);
            
            if (result.isMatch()) {
                log.info("Settlement {} reconciled successfully", settlement.getSettlementId());
            } else {
                log.warn("Settlement {} reconciliation failed. Discrepancies: {}", 
                    settlement.getSettlementId(), result.getDiscrepancies());
                
                // Create reconciliation alert
                createReconciliationAlert(settlement, result);
            }

        } catch (Exception e) {
            log.error("Error reconciling settlement: {}", settlement.getSettlementId(), e);
        }
    }

    private Map<String, Object> fetchPSPReport(String pspReference) {
        // Simulate fetching PSP report
        // In production, make actual API call to PSP
        Map<String, Object> report = new HashMap<>();
        report.put("reference", pspReference);
        report.put("totalAmount", settlementRepository.findByPspReference(pspReference)
            .map(Settlement::getTotalAmount)
            .orElse(BigDecimal.ZERO));
        report.put("transactionCount", settlementRepository.findByPspReference(pspReference)
            .map(Settlement::getTransactionCount)
            .orElse(0));
        report.put("status", "COMPLETED");
        report.put("timestamp", LocalDateTime.now());
        
        return report;
    }

    private ReconciliationResult compareRecords(List<SettlementItem> internalItems, Map<String, Object> pspReport) {
        ReconciliationResult result = new ReconciliationResult();
        List<String> discrepancies = new ArrayList<>();

        // Calculate internal totals
        BigDecimal internalTotal = internalItems.stream()
            .map(SettlementItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int internalCount = internalItems.size();

        // Get external totals
        BigDecimal externalTotal = new BigDecimal(pspReport.get("totalAmount").toString());
        int externalCount = (Integer) pspReport.get("transactionCount");

        // Compare totals
        if (internalTotal.compareTo(externalTotal) != 0) {
            discrepancies.add(String.format("Amount mismatch: Internal=%s, External=%s", 
                internalTotal, externalTotal));
        }

        if (internalCount != externalCount) {
            discrepancies.add(String.format("Count mismatch: Internal=%d, External=%d", 
                internalCount, externalCount));
        }

        // Compare individual transactions (simplified)
        for (SettlementItem item : internalItems) {
            // In production, match each transaction with PSP records
            if (item.getStatus() != SettlementItemStatus.INCLUDED) {
                discrepancies.add(String.format("Transaction %s not marked as included", item.getPaymentId()));
            }
        }

        result.setMatch(discrepancies.isEmpty());
        result.setDiscrepancies(discrepancies);
        result.setInternalTotal(internalTotal);
        result.setExternalTotal(externalTotal);
        result.setInternalCount(internalCount);
        result.setExternalCount(externalCount);

        return result;
    }

    private void createReconciliationAlert(Settlement settlement, ReconciliationResult result) {
        // In production, create alert in notification system or separate alerting service
        log.error("RECONCILIATION ALERT - Settlement: {}, Discrepancies: {}", 
            settlement.getSettlementId(), result.getDiscrepancies());
        
        // Could also send notification to operations team
        // notificationService.sendReconciliationAlert(settlement, result);
    }

    public ReconciliationResult manualReconciliation(String settlementId) {
        Settlement settlement = settlementRepository.findBySettlementId(settlementId)
            .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));
        
        return reconcileSettlementAndGetResult(settlement);
    }

    private ReconciliationResult reconcileSettlementAndGetResult(Settlement settlement) {
        List<SettlementItem> internalItems = settlementItemRepository.findBySettlementId(settlement.getSettlementId());
        Map<String, Object> pspReport = fetchPSPReport(settlement.getPspReference());
        return compareRecords(internalItems, pspReport);
    }

    public static class ReconciliationResult {
        private boolean match;
        private List<String> discrepancies;
        private BigDecimal internalTotal;
        private BigDecimal externalTotal;
        private int internalCount;
        private int externalCount;

        public boolean isMatch() {
            return match;
        }

        public void setMatch(boolean match) {
            this.match = match;
        }

        public List<String> getDiscrepancies() {
            return discrepancies;
        }

        public void setDiscrepancies(List<String> discrepancies) {
            this.discrepancies = discrepancies;
        }

        public BigDecimal getInternalTotal() {
            return internalTotal;
        }

        public void setInternalTotal(BigDecimal internalTotal) {
            this.internalTotal = internalTotal;
        }

        public BigDecimal getExternalTotal() {
            return externalTotal;
        }

        public void setExternalTotal(BigDecimal externalTotal) {
            this.externalTotal = externalTotal;
        }

        public int getInternalCount() {
            return internalCount;
        }

        public void setInternalCount(int internalCount) {
            this.internalCount = internalCount;
        }

        public int getExternalCount() {
            return externalCount;
        }

        public void setExternalCount(int externalCount) {
            this.externalCount = externalCount;
        }
    }
}
