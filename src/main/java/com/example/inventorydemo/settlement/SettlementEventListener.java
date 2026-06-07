package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 结算模块事件监听器
 *
 * 监听采购/销售完结事件，委托 SettlementService 写入结算库（跨数据库）。
 * 使用 @ApplicationModuleListener 保证可靠投递，Service 方法使用 @Transactional(REQUIRES_NEW)
 * 创建独立的结算事务，确保 JPA EntityManager 正确参与。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementService.createFromPurchase(event);
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementService.createFromSale(event);
    }
}
