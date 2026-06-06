package com.example.inventorydemo.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    @Transactional
    public Settlement createSettlement(Long orderId, SettlementType type, BigDecimal amount) {
        Settlement settlement = new Settlement();
        settlement.setOrderId(orderId);
        settlement.setType(type);
        settlement.setAmount(amount);
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setCreatedAt(LocalDateTime.now());
        return settlementRepository.save(settlement);
    }

    @Transactional
    public Settlement settle(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new RuntimeException("Settlement is not in pending status");
        }
        settlement.setStatus(SettlementStatus.SETTLED);
        return settlementRepository.save(settlement);
    }
}
