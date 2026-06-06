package com.example.inventorydemo.sale;

import com.example.inventorydemo.event.CrossDb;

import java.math.BigDecimal;

@CrossDb(targetModules = {"settlement"})
public record SaleOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
