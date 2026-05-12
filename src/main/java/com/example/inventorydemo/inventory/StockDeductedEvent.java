package com.example.inventorydemo.inventory;

public record StockDeductedEvent(Long productId, int quantity) {
}