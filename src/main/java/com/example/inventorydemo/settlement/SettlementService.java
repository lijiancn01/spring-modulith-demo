package com.example.inventorydemo.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    public Settlement createSettlement(Long orderId, SettlementType type, BigDecimal amount) {
        log.info("createSettlement 被调用: orderId={}, type={}, amount={}", orderId, type, amount);
        if (settlementRepository.existsByOrderIdAndType(orderId, type)) {
            log.info("结算单已存在，跳过: orderId={}, type={}", orderId, type);
            return settlementRepository.findByOrderIdAndType(orderId, type).orElse(null);
        }
        Settlement settlement = new Settlement();
        settlement.setOrderId(orderId);
        settlement.setType(type);
        settlement.setAmount(amount);
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setCreatedAt(LocalDateTime.now());
        log.info("保存结算单: orderId={}, type={}, amount={}", orderId, type, amount);
        return settlementRepository.save(settlement);
    }

    @Transactional("settlementTransactionManager")
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
