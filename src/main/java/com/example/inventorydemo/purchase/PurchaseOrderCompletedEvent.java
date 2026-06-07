package com.example.inventorydemo.purchase;

import org.springframework.modulith.events.Externalized;

import java.math.BigDecimal;

@Externalized("purchase-order-completed::#{orderId()}")
public record PurchaseOrderCompletedEvent(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount
) {}
