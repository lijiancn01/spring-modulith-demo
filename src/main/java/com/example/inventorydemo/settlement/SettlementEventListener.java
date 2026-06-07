package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 结算模块事件监听器（Spring 事件模式）
 *
 * 通过 Spring Modulith 监听同 JVM 内的采购/销售完结事件，
 * 写入结算库（跨数据库）。失败时自动重试。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "spring", matchIfMissing = true)
public class SettlementEventListener {

    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;

    public SettlementEventListener(
            @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
            @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 1000))
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
                    event.orderId(), "PAYABLE", event.quantity(), event.totalAmount());
        });
    }

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 1000))
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
                    event.orderId(), "RECEIVABLE", event.quantity(), event.totalAmount());
        });
    }
}
