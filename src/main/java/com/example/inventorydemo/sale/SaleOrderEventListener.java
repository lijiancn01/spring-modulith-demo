package com.example.inventorydemo.sale;

import com.example.inventorydemo.settlement.SettlementCompletedEvent;
import com.example.inventorydemo.settlement.SettlementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 销售模块事件监听器
 *
 * 监听结算模块的结算完成事件（事务提交后），回写结算量和结算金额到销售单。
 * 幂等保护：如果 settledQuantity > 0 说明已处理过，跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleOrderEventListener {

    private final SaleOrderRepository saleOrderRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSettlementCompleted(SettlementCompletedEvent event) {
        if (event.type() != SettlementType.RECEIVABLE) {
            return;
        }
        log.info("销售模块收到结算完成事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.amount());

        SaleOrder order = saleOrderRepository.findById(event.orderId())
                .orElseThrow(() -> new RuntimeException("Sale order not found: " + event.orderId()));

        if (order.getSettledQuantity() > 0) {
            log.info("销售单已结算，跳过: orderId={}", event.orderId());
            return;
        }

        order.setSettledQuantity(event.quantity());
        order.setSettledAmount(event.amount());
        saleOrderRepository.save(order);
        log.info("销售单结算信息已回写: orderId={}, settledQty={}, settledAmt={}",
                event.orderId(), event.quantity(), event.amount());
    }
}
