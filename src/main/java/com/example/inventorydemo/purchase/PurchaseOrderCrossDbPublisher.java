package com.example.inventorydemo.purchase;

import com.example.inventorydemo.event.CrossDbEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 采购完成事件 → 跨库写入结算模块的 EVENT_PUBLICATION 表
 *
 * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保：
 * 1. 生产者事务（订单状态更新 + 本地事件落表）已提交
 * 2. 此时再写入目标库的事件表，如果写入失败，本地事件表中仍有记录可重试
 *
 * 写入目标库后，CrossDbEventConsumer 定时触发 resubmit，
 * 结算模块的 @ApplicationModuleListener 自动消费并归档
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderCrossDbPublisher {

    private final CrossDbEventPublisher crossDbEventPublisher;
    // 跨库场景下，这里注入结算库的 JdbcTemplate（当前单库演示用同一个）
    private final JdbcTemplate jdbcTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseCompleted(PurchaseOrderCompletedEvent event) {
        crossDbEventPublisher.publish(
                event,
                "com.example.inventorydemo.settlement.SettlementEventListener.handlePurchaseCompleted",
                jdbcTemplate);
    }
}
