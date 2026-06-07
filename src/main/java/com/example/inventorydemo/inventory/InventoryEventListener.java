package com.example.inventorydemo.inventory;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("库存模块收到采购完结事件: productId={}, quantity={}", event.productId(), event.quantity());
        inventoryService.addStock(event.productId(), event.quantity());
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("库存模块收到销售完结事件: productId={}, quantity={}", event.productId(), event.quantity());
        inventoryService.deductStock(event.productId(), event.quantity());
    }
}
