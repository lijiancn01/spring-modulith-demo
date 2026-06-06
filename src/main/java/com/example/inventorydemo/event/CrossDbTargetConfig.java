package com.example.inventorydemo.event;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 跨库事件目标注册配置
 *
 * 将目标模块名映射到对应的 JdbcTemplate，
 * 并按事件类型注册对应的 listenerId（必须与 Spring Modulith 实际生成的 listenerId 一致，
 * 即包含方法全限定名和参数类型）。
 */
@Configuration
@RequiredArgsConstructor
public class CrossDbTargetConfig {

    private final CrossDbTargetRegistry targetRegistry;
    // 跨库场景下，这里注入结算库的 JdbcTemplate（当前单库演示用同一个）
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void registerTargets() {
        targetRegistry.register("settlement", jdbcTemplate);
        targetRegistry.registerListenerId("settlement",
                PurchaseOrderCompletedEvent.class.getName(),
                "com.example.inventorydemo.settlement.SettlementEventListener.handlePurchaseCompleted(com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent)");
        targetRegistry.registerListenerId("settlement",
                SaleOrderCompletedEvent.class.getName(),
                "com.example.inventorydemo.settlement.SettlementEventListener.handleSaleCompleted(com.example.inventorydemo.sale.SaleOrderCompletedEvent)");
    }
}
