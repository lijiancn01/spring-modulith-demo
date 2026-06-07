package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 结算模块 Kafka 事件监听器（Kafka 模式）
 *
 * 通过 Kafka 消费跨数据库事件，使用 @RetryableTopic 实现自动重试。
 * 重试策略：4次尝试（1次原始+3次重试），指数退避，失败后进入 DLT。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class SettlementKafkaListener {

    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;

    public SettlementKafkaListener(
            @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
            @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.MULTIPLE_TOPICS,
            retryTopicSuffix = "-retry", dltTopicSuffix = "-dlt",
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "purchase-order-completed", groupId = "settlement-group")
    public void handlePurchaseCompleted(Map<String, Object> event,
                                        @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attempts) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        int quantity = ((Number) event.get("quantity")).intValue();
        BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());
        log.info("结算模块收到采购完结事件（Kafka）: orderId={}, quantity={}, amount={}, attempts={}", orderId, quantity, totalAmount, attempts);
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
                    orderId, "PAYABLE", quantity, totalAmount);
        });
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.MULTIPLE_TOPICS,
            retryTopicSuffix = "-retry", dltTopicSuffix = "-dlt",
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "sale-order-completed", groupId = "settlement-group")
    public void handleSaleCompleted(Map<String, Object> event,
                                    @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attempts) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        int quantity = ((Number) event.get("quantity")).intValue();
        BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").toString());
        log.info("结算模块收到销售完结事件（Kafka）: orderId={}, quantity={}, amount={}, attempts={}", orderId, quantity, totalAmount, attempts);
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
                    orderId, "RECEIVABLE", quantity, totalAmount);
        });
    }
}
