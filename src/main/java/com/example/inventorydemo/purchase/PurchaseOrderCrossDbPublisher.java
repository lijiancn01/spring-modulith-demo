package com.example.inventorydemo.purchase;

import com.example.inventorydemo.event.CrossDbEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 采购完成事件 → 跨库发布到结算模块
 *
 * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保：
 * 1. 生产者事务（订单状态更新 + 本地事件落表）已提交
 * 2. 此时再写入跨库事件表，如果写入失败，本地事件表中仍有记录可重试
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderCrossDbPublisher {

    private final CrossDbEventPublisher crossDbEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseCompleted(PurchaseOrderCompletedEvent event) {
        crossDbEventPublisher.publish(event, "settlement", "purchase");
    }
}
