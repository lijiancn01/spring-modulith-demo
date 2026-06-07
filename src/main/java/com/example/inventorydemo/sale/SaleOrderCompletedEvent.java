package com.example.inventorydemo.sale;

import org.springframework.modulith.events.Externalized;

import java.math.BigDecimal;

@Externalized("sale-order-completed::#{orderId()}")
public record SaleOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
