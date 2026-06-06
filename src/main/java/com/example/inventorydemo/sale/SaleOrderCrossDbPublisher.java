package com.example.inventorydemo.sale;

import com.example.inventorydemo.event.CrossDbEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 销售完成事件 → 跨库发布到结算模块
 */
@Component
@RequiredArgsConstructor
public class SaleOrderCrossDbPublisher {

    private final CrossDbEventPublisher crossDbEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(SaleOrderCompletedEvent event) {
        crossDbEventPublisher.publish(event, "settlement", "sale");
    }
}
