package com.andryalbert.settlement.service;

import com.andryalbert.settlement.entity.Settlement;
import com.andryalbert.settlement.entity.SettlementItem;
import com.andryalbert.settlement.entity.SettlementItemStatus;
import com.andryalbert.settlement.entity.SettlementStatus;
// import com.andryalbert.settlement.kafka.producer.SettlementResponseProducer;
import com.andryalbert.settlement.repository.SettlementItemRepository;
import com.andryalbert.settlement.repository.SettlementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    // private final SettlementResponseProducer settlementResponseProducer;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${settlement.psp.endpoint}")
    private String pspEndpoint;

    @Value("${settlement.psp.api-key}")
    private String pspApiKey;

    @Value("${settlement.batch.size}")
    private int batchSize;

    // In-memory store for pending payments to be settled
    private final Map<String, JsonNode> pendingPayments = new ConcurrentHashMap<>();

    @Transactional
    public void processPaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String paymentId = root.get("paymentId").asText();
            String merchantId = root.get("merchantId").asText();
            BigDecimal amount = new BigDecimal(root.get("amount").asText());
            String currency = root.get("currency").asText();

            if (eventType.equals("PAYMENT_AUTHORIZED") || eventType.equals("PAYMENT_SETTLED")) {
                // Add to pending settlements
                pendingPayments.put(paymentId, root);
                log.info("Payment {} added to pending settlements", paymentId);
            } else if (eventType.equals("PAYMENT_REFUNDED")) {
                // Remove from pending settlements if refunded
                pendingPayments.remove(paymentId);
                log.info("Payment {} removed from pending settlements due to refund", paymentId);
            }

        } catch (Exception e) {
            log.error("Error processing payment event for settlement", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    @Transactional
    public void processDailySettlements() {
        log.info("Starting daily settlement processing");

        // Group pending payments by merchant and currency
        Map<String, List<JsonNode>> groupedPayments = pendingPayments.values().stream()
            .collect(Collectors.groupingBy(payment -> {
                String merchantId = payment.get("merchantId").asText();
                String currency = payment.get("currency").asText();
                return merchantId + "-" + currency;
            }));

        // Create settlement batch for each merchant/currency combination
        for (Map.Entry<String, List<JsonNode>> entry : groupedPayments.entrySet()) {
            String key = entry.getKey();
            List<JsonNode> payments = entry.getValue();

            if (payments.size() >= batchSize) {
                String[] parts = key.split("-");
                String merchantId = parts[0];
                String currency = parts[1];

                createSettlementBatch(merchantId, currency, payments);
            }
        }

        log.info("Daily settlement processing completed");
    }

    @Transactional
    public void createSettlementBatch(String merchantId, String currency, List<JsonNode> payments) {
        String settlementId = Settlement.generateSettlementId();
        LocalDateTime settlementDate = LocalDateTime.now();
        LocalDateTime expectedCompletion = settlementDate.plusDays(2); // T+2 settlement

        BigDecimal totalAmount = payments.stream()
            .map(payment -> payment.get("amount").decimalValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFee = totalAmount.multiply(new BigDecimal("0.029")); // 2.9% fee
        BigDecimal netAmount = totalAmount.subtract(totalFee);

        Settlement settlement = new Settlement(
            settlementId,
            merchantId,
            currency,
            totalAmount,
            payments.size(),
            settlementDate,
            expectedCompletion,
            "BANK-ACC-" + merchantId, // In production, get from merchant profile
            "ROUTING-" + merchantId
        );

        settlement = settlementRepository.save(settlement);

        // Create settlement items
        List<SettlementItem> items = new ArrayList<>();
        for (JsonNode payment : payments) {
            String paymentId = payment.get("paymentId").asText();
            BigDecimal amount = payment.get("amount").decimalValue();
            BigDecimal fee = amount.multiply(new BigDecimal("0.029"));
            BigDecimal itemNetAmount = amount.subtract(fee);

            SettlementItem item = new SettlementItem(
                SettlementItem.generateItemId(),
                paymentId,
                amount,
                currency,
                fee,
                itemNetAmount
            );

            items.add(item);
            
            // Remove from pending after adding to settlement
            pendingPayments.remove(paymentId);
        }

        settlementItemRepository.saveAll(items);

        // Submit to PSP
        submitToPSP(settlement);

        log.info("Settlement batch created: {} for merchant: {} with {} transactions", 
            settlementId, merchantId, payments.size());
    }

    @Transactional
    public void submitToPSP(Settlement settlement) {
        try {
            settlement.setStatus(SettlementStatus.PROCESSING);
            settlementRepository.save(settlement);

            // Prepare PSP request
            Map<String, Object> pspRequest = Map.of(
                "settlement_id", settlement.getSettlementId(),
                "merchant_id", settlement.getMerchantId(),
                "amount", settlement.getTotalAmount(),
                "currency", settlement.getCurrency(),
                "transaction_count", settlement.getTransactionCount(),
                "bank_account", settlement.getBankAccount(),
                "routing_number", settlement.getRoutingNumber(),
                "settlement_date", settlement.getSettlementDate().toString()
            );

            // Simulate PSP API call (in production, use actual WebClient call)
            // String response = webClient.post()
            //     .uri(pspEndpoint)
            //     .header("Authorization", "Bearer " + pspApiKey)
            //     .bodyValue(pspRequest)
            //     .retrieve()
            //     .bodyToMono(String.class)
            //     .block();

            // Simulated response
            String pspReference = "PSP-REF-" + System.currentTimeMillis();
            
            settlement.setStatus(SettlementStatus.SUBMITTED);
            settlement.setPspReference(pspReference);
            settlement.setPspResponse("Settlement submitted successfully");
            settlementRepository.save(settlement);

            // Publish settlement submitted event
            // settlementResponseProducer.publishSettlementEvent(settlement.getSettlementId(), "SUBMITTED", pspReference);

            log.info("Settlement {} submitted to PSP with reference: {}", settlement.getSettlementId(), pspReference);

        } catch (Exception e) {
            log.error("Error submitting settlement to PSP: {}", settlement.getSettlementId(), e);
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setErrorMessage(e.getMessage());
            settlementRepository.save(settlement);
        }
    }

    @Scheduled(fixedDelay = 300000) // Check every 5 minutes
    @Transactional
    public void checkSettlementStatus() {
        List<Settlement> submittedSettlements = settlementRepository.findByStatus(SettlementStatus.SUBMITTED);
        
        for (Settlement settlement : submittedSettlements) {
            if (settlement.getExpectedCompletionDate().isBefore(LocalDateTime.now())) {
                // Simulate PSP status check (in production, call PSP API)
                boolean completed = simulatePSPStatusCheck(settlement.getPspReference());
                
                if (completed) {
                    settlement.setStatus(SettlementStatus.COMPLETED);
                    settlement.setPspResponse("Settlement completed successfully");
                    
                    // Publish settlement completed event for all payments in this settlement
                    List<SettlementItem> items = settlementItemRepository.findBySettlementId(settlement.getSettlementId());
                    for (SettlementItem item : items) {
                        // settlementResponseProducer.publishSettlementResponse(item.getPaymentId(), true, settlement.getSettlementId());
                        item.setStatus(SettlementItemStatus.INCLUDED);
                    }
                    
                    settlementItemRepository.saveAll(items);
                    settlementRepository.save(settlement);
                    
                    log.info("Settlement {} completed", settlement.getSettlementId());
                }
            }
        }
    }

    private boolean simulatePSPStatusCheck(String pspReference) {
        // Simulate PSP status check - in production, call actual PSP API
        return Math.random() > 0.3; // 70% chance of completion
    }

    public Settlement getSettlement(String settlementId) {
        return settlementRepository.findBySettlementId(settlementId)
            .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));
    }

    public List<Settlement> getMerchantSettlements(String merchantId) {
        return settlementRepository.findByMerchantIdAndStatusOrderBySettlementDateDesc(merchantId, SettlementStatus.COMPLETED);
    }
}
