package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 结算模块事件监听器
 *
 * 跨库场景下，事件由 CrossDbEventConsumer 轮询 cross_db_events 表后
 * 通过 Spring 事件总线重新发布，此处 @EventListener 接收并处理。
 *
 * 必须幂等：因为跨库事件可能重复投递
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    @EventListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.PAYABLE)) {
            log.info("采购结算单已存在，跳过: orderId={}", event.orderId());
            return;
        }
        settlementService.createSettlement(event.orderId(), SettlementType.PAYABLE, event.totalAmount());
    }

    @EventListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.RECEIVABLE)) {
            log.info("销售结算单已存在，跳过: orderId={}", event.orderId());
            return;
        }
        settlementService.createSettlement(event.orderId(), SettlementType.RECEIVABLE, event.totalAmount());
    }
}
