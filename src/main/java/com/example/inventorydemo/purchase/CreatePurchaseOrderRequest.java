package com.example.inventorydemo.purchase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseOrderRequest(
        @NotNull(message = "商品ID不能为空") Long productId,
        @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为1") Integer quantity,
        @NotNull(message = "单价不能为空") @Min(value = 0, message = "单价不能为负数") java.math.BigDecimal unitPrice
) {}
