package com.example.inventorydemo.sale;

import java.math.BigDecimal;

public record SaleOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
