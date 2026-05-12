package com.example.inventorydemo.inventory;

import org.springframework.modulith.events.Externalized;

@Externalized
public record StockDeductedEvent(Long productId, int quantity) {
}