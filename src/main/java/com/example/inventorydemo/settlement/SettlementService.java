package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SettlementService {

    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(@Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                             @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager,
                             ApplicationEventPublisher eventPublisher) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
        this.eventPublisher = eventPublisher;
    }

    public List<Settlement> getAllSettlements() {
        return settlementJdbcTemplate.query(
                "SELECT * FROM settlements ORDER BY id",
                new BeanPropertyRowMapper<>(Settlement.class));
    }

    public void createSettlement(Long orderId, SettlementType type, BigDecimal amount, int quantity) {
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
                    orderId, type.name(), quantity, amount);
        });
    }

    public Settlement settle(Long id) {
        settlementTxTemplate.executeWithoutResult(status -> {
            int updated = settlementJdbcTemplate.update(
                    "UPDATE settlements SET status = 'SETTLED' WHERE id = ? AND status = 'PENDING'",
                    id);
            if (updated == 0) {
                throw new RuntimeException("Settlement not found or not in pending status");
            }
        });
        Settlement settlement = settlementJdbcTemplate.queryForObject(
                "SELECT * FROM settlements WHERE id = ?",
                new BeanPropertyRowMapper<>(Settlement.class), id);

        // 结算完成后，发布事件通知采购/销售模块回写结算量和结算金额
        eventPublisher.publishEvent(new SettlementCompletedEvent(
                settlement.getOrderId(),
                settlement.getQuantity(),
                settlement.getAmount(),
                settlement.getType()
        ));
        log.info("结算完成事件已发布: orderId={}, quantity={}, amount={}, type={}",
                settlement.getOrderId(), settlement.getQuantity(), settlement.getAmount(), settlement.getType());

        return settlement;
    }
}
