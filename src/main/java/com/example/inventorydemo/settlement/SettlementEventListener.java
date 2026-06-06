package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    @ApplicationModuleListener
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        settlementService.createSettlement(event.orderId(), SettlementType.PAYABLE, event.totalAmount());
    }

    @ApplicationModuleListener
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        settlementService.createSettlement(event.orderId(), SettlementType.RECEIVABLE, event.totalAmount());
    }
}
