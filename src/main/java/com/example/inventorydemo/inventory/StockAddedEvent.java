package com.example.inventorydemo.inventory;

import org.springframework.modulith.events.Externalized;

@Externalized
public record StockAddedEvent(Long productId, int quantity) {
}