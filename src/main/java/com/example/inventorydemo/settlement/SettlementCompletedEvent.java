package com.example.inventorydemo.settlement;

import java.math.BigDecimal;

/**
 * 结算完成事件
 *
 * 结算模块结算完成后发布，采购/销售模块监听此事件回写结算量和结算金额。
 */
public record SettlementCompletedEvent(
        Long orderId,
        int quantity,
        BigDecimal amount,
        SettlementType type
) {}
