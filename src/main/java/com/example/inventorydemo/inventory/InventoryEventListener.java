package com.example.inventorydemo.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @ApplicationModuleListener
    public void handleStockAdded(StockAddedEvent event) {
        inventoryService.addStock(event.productId(), event.quantity());
    }

    @ApplicationModuleListener
    public void handleStockDeducted(StockDeductedEvent event) {
        inventoryService.deductStock(event.productId(), event.quantity());
    }
}