package com.example.inventorydemo.inventory;

public record StockAddedEvent(Long productId, int quantity) {
}