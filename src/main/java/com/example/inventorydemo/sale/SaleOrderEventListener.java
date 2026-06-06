package com.example.inventorydemo.sale;

import com.example.inventorydemo.settlement.SettlementCompletedEvent;
import com.example.inventorydemo.settlement.SettlementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 销售模块事件监听器
 *
 * 监听结算模块的结算完成事件，回写结算量和结算金额到销售单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleOrderEventListener {

    private final SaleOrderRepository saleOrderRepository;

    @EventListener
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
