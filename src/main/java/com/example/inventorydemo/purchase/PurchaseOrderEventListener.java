package com.example.inventorydemo.purchase;

import com.example.inventorydemo.EntityNotFoundException;
import com.example.inventorydemo.InvalidStatusException;
import com.example.inventorydemo.settlement.SettlementCompletedEvent;
import com.example.inventorydemo.settlement.SettlementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderEventListener {

    private final PurchaseOrderService purchaseOrderService;

    @ApplicationModuleListener
    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {EntityNotFoundException.class, InvalidStatusException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleSettlementCompleted(SettlementCompletedEvent event) {
        if (event.type() != SettlementType.PAYABLE) {
            return;
        }
        log.info("采购模块收到结算完成事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.amount());
        purchaseOrderService.applySettlement(event);
    }
}
