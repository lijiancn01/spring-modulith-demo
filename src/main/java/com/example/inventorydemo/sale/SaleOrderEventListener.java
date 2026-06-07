package com.example.inventorydemo.sale;

import com.example.inventorydemo.settlement.SettlementCompletedEvent;
import com.example.inventorydemo.settlement.SettlementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * 销售模块事件监听器（Spring 事件模式）
 *
 * 监听结算模块的结算完成事件，回写结算量和结算金额到销售单。失败时自动重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "spring", matchIfMissing = true)
public class SaleOrderEventListener {

    private final SaleOrderRepository saleOrderRepository;

    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleSettlementCompleted(SettlementCompletedEvent event) {
        if (event.type() != SettlementType.RECEIVABLE) {
            return;
        }
        log.info("销售模块收到结算完成事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.amount());

        SaleOrder order = saleOrderRepository.findById(event.orderId())
                .orElseThrow(() -> new RuntimeException("Sale order not found: " + event.orderId()));
        order.setSettledQuantity(order.getSettledQuantity() + event.quantity());
        order.setSettledAmount(order.getSettledAmount().add(event.amount()));
        saleOrderRepository.save(order);
    }
}
