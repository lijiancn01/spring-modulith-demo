package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 结算模块事件监听器
 *
 * 通过 Kafka 消费跨数据库事件。
 * 采购/销售模块在另一个数据库，事件通过 Spring Modulith 外化到 Kafka，
 * 结算模块从 Kafka 消费事件并写入结算库。
 */
@Slf4j
@Component
public class SettlementEventListener {

    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;

    public SettlementEventListener(
            @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
            @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    @KafkaListener(topics = "purchase-order-completed", groupId = "settlement-group")
    public void handlePurchaseCompleted(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());
        log.info("结算模块收到采购完结事件（Kafka）: orderId={}, amount={}", orderId, totalAmount);
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())",
                    orderId, "PAYABLE", totalAmount);
        });
    }

    @KafkaListener(topics = "sale-order-completed", groupId = "settlement-group")
    public void handleSaleCompleted(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());
        log.info("结算模块收到销售完结事件（Kafka）: orderId={}, amount={}", orderId, totalAmount);
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())",
                    orderId, "RECEIVABLE", totalAmount);
        });
    }
}
