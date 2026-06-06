package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 结算模块事件监听器
 *
 * 跨库场景下，事件由 CrossDbEventPublisher 写入本库的 EVENT_PUBLICATION 表，
 * CrossDbEventConsumer 定时触发 resubmit，事件重新发布到 Spring 事件总线，
 * 此处 @ApplicationModuleListener 接收并消费。
 *
 * 消费成功后 Spring Modulith 自动归档到 EVENT_PUBLICATION_ARCHIVE（同库同事务）。
 * 必须幂等：因为跨库事件可能重复投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.PAYABLE)) {
            log.info("采购结算单已存在，跳过: orderId={}", event.orderId());
            return;
        }
        settlementService.createSettlement(event.orderId(), SettlementType.PAYABLE, event.totalAmount());
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.RECEIVABLE)) {
            log.info("销售结算单已存在，跳过: orderId={}", event.orderId());
            return;
        }
        settlementService.createSettlement(event.orderId(), SettlementType.RECEIVABLE, event.totalAmount());
    }
}
