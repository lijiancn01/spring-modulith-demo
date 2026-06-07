package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public SettlementService(@Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                             @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    public List<Settlement> getAllSettlements() {
        return settlementJdbcTemplate.query(
                "SELECT * FROM settlements ORDER BY id",
                new BeanPropertyRowMapper<>(Settlement.class));
    }

    public void createSettlement(Long orderId, SettlementType type, BigDecimal amount) {
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())",
                    orderId, type.name(), amount);
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
        return settlementJdbcTemplate.queryForObject(
                "SELECT * FROM settlements WHERE id = ?",
                new BeanPropertyRowMapper<>(Settlement.class), id);
    }
}
