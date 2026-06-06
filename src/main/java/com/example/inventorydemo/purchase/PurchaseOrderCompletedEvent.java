package com.example.inventorydemo.purchase;

import com.example.inventorydemo.event.CrossDb;

import java.math.BigDecimal;

@CrossDb(targetModules = {"settlement"})
public record PurchaseOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
