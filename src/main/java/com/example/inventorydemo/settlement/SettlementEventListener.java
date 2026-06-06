package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, amount={}", event.orderId(), event.totalAmount());
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())",
                    event.orderId(), "PAYABLE", event.totalAmount());
        });
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, amount={}", event.orderId(), event.totalAmount());
        settlementTxTemplate.executeWithoutResult(status -> {
            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, amount, status, created_at) VALUES (?, ?, ?, 'PENDING', NOW())",
                    event.orderId(), "RECEIVABLE", event.totalAmount());
        });
    }
}
