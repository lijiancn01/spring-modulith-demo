package com.example.inventorydemo.sale;

import com.example.inventorydemo.event.CrossDbEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 销售完成事件 → 跨库写入结算模块的 EVENT_PUBLICATION 表
 */
@Component
@RequiredArgsConstructor
public class SaleOrderCrossDbPublisher {

    private final CrossDbEventPublisher crossDbEventPublisher;
    // 跨库场景下，这里注入结算库的 JdbcTemplate（当前单库演示用同一个）
    private final JdbcTemplate jdbcTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(SaleOrderCompletedEvent event) {
        crossDbEventPublisher.publish(
                event,
                "com.example.inventorydemo.settlement.SettlementEventListener.handleSaleCompleted",
                jdbcTemplate);
    }
}
