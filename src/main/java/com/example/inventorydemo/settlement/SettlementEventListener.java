package com.example.inventorydemo.settlement;

import com.example.inventorydemo.EntityNotFoundException;
import com.example.inventorydemo.InvalidStatusException;
import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * 结算模块事件监听器
 *
 * 监听采购/销售完结事件，委托 SettlementService 写入结算库（跨数据库）。
 * - @ApplicationModuleListener: 事件持久化 + AFTER_COMMIT 投递，保证可靠
 * - @Retryable: 瞬态异常即时重试（乐观锁冲突、数据库瞬态故障等）
 * - EventRetryScheduler: 定时兜底重投递未完成事件
 * - 业务异常（EntityNotFound/InvalidStatus）不重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    @ApplicationModuleListener
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {EntityNotFoundException.class, InvalidStatusException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementService.createFromPurchase(event);
    }

    @ApplicationModuleListener
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {EntityNotFoundException.class, InvalidStatusException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());
        settlementService.createFromSale(event);
    }
}
