package com.example.inventorydemo.purchase;

import java.math.BigDecimal;

public record PurchaseOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
