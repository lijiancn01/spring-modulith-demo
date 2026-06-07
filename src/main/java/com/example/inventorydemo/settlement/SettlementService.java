package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final JdbcTemplate settlementJdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(SettlementRepository settlementRepository,
                             @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                             ApplicationEventPublisher eventPublisher) {
        this.settlementRepository = settlementRepository;
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(value = "settlementTransactionManager", readOnly = true)
    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    @Transactional("settlementTransactionManager")
    public Settlement settle(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new RuntimeException("Settlement not in pending status");
        }
        settlementJdbcTemplate.update(
                "UPDATE settlements SET status = ? WHERE id = ?",
                SettlementStatus.SETTLED.name(), id);
        settlement.setStatus(SettlementStatus.SETTLED);

        // 在结算事务内发布事件，由 @TransactionalEventListener 在事务提交后处理
        eventPublisher.publishEvent(new SettlementCompletedEvent(
                settlement.getOrderId(),
                settlement.getQuantity(),
                settlement.getAmount(),
                settlement.getType()
        ));
        log.info("结算完成事件已发布: orderId={}, quantity={}, amount={}, type={}",
                settlement.getOrderId(), settlement.getQuantity(),
                settlement.getAmount(), settlement.getType());

        return settlement;
    }
}
