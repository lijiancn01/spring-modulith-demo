package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class SettlementEventListener {

    private final SettlementService settlementService;
    private final TransactionTemplate settlementTxTemplate;

    public SettlementEventListener(
            SettlementService settlementService,
            @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementService = settlementService;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, amount={}", event.orderId(), event.totalAmount());
        try {
            settlementTxTemplate.executeWithoutResult(status ->
                    settlementService.createSettlement(event.orderId(), SettlementType.PAYABLE, event.totalAmount())
            );
            log.info("结算单创建成功: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("结算单创建失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, amount={}", event.orderId(), event.totalAmount());
        try {
            settlementTxTemplate.executeWithoutResult(status ->
                    settlementService.createSettlement(event.orderId(), SettlementType.RECEIVABLE, event.totalAmount())
            );
            log.info("结算单创建成功: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("结算单创建失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }
}
