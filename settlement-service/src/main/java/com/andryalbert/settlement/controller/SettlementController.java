package com.andryalbert.settlement.controller;

import com.andryalbert.settlement.entity.Settlement;
import com.andryalbert.settlement.service.ReconciliationService;
import com.andryalbert.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettlementController {

    private final SettlementService settlementService;
    private final ReconciliationService reconciliationService;

    @GetMapping("/{settlementId}")
    public ResponseEntity<Settlement> getSettlement(@PathVariable String settlementId) {
        Settlement settlement = settlementService.getSettlement(settlementId);
        return ResponseEntity.ok(settlement);
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Settlement>> getMerchantSettlements(@PathVariable String merchantId) {
        List<Settlement> settlements = settlementService.getMerchantSettlements(merchantId);
        return ResponseEntity.ok(settlements);
    }

    @PostMapping("/process")
    public ResponseEntity<String> processSettlements() {
        settlementService.processDailySettlements();
        return ResponseEntity.ok("Settlement processing initiated");
    }

    @PostMapping("/reconcile/{settlementId}")
    public ResponseEntity<Map<String, Object>> reconcileSettlement(@PathVariable String settlementId) {
        var result = reconciliationService.manualReconciliation(settlementId);
        return ResponseEntity.ok(Map.of(
            "match", result.isMatch(),
            "discrepancies", result.getDiscrepancies(),
            "internalTotal", result.getInternalTotal(),
            "externalTotal", result.getExternalTotal(),
            "internalCount", result.getInternalCount(),
            "externalCount", result.getExternalCount()
        ));
    }

    @PostMapping("/reconcile/daily")
    public ResponseEntity<String> performDailyReconciliation() {
        reconciliationService.performDailyReconciliation();
        return ResponseEntity.ok("Daily reconciliation initiated");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Settlement Service is healthy");
    }
}
