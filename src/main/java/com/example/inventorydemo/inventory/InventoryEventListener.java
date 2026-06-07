package com.example.inventorydemo.inventory;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        inventoryService.addStock(event.productId(), event.quantity());
    }

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        inventoryService.deductStock(event.productId(), event.quantity());
    }
}
