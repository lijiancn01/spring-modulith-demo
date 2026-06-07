package com.example.inventorydemo.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "商品编码不能为空") String code,
        @NotBlank(message = "商品名称不能为空") String name,
        String description,
        @NotNull(message = "价格不能为空") @Min(value = 0, message = "价格不能为负数") BigDecimal price
) {}
