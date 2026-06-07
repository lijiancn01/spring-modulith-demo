package com.example.inventorydemo.purchase;

import com.example.inventorydemo.EntityNotFoundException;
import com.example.inventorydemo.InvalidStatusException;
import com.example.inventorydemo.settlement.SettlementCompletedEvent;
import com.example.inventorydemo.settlement.SettlementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(Long productId, int quantity, BigDecimal unitPrice) {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(PurchaseOrderStatus.PENDING);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder completePurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("采购订单不存在: " + id));
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new InvalidStatusException("采购订单状态不是待处理: " + id);
        }
        order.setStatus(PurchaseOrderStatus.COMPLETED);
        eventPublisher.publishEvent(new PurchaseOrderCompletedEvent(
                order.getId(), order.getProductId(), order.getQuantity(),
                order.getUnitPrice(), order.getTotalAmount()));
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("采购订单不存在: " + id));
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new InvalidStatusException("采购订单状态不是待处理: " + id);
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySettlement(SettlementCompletedEvent event) {
        if (event.type() != SettlementType.PAYABLE) {
            return;
        }
        PurchaseOrder order = purchaseOrderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("采购订单不存在: " + event.orderId()));
        if (order.getSettledQuantity() > 0) {
            return;
        }
        order.setSettledQuantity(event.quantity());
        order.setSettledAmount(event.amount());
        purchaseOrderRepository.save(order);
        log.info("采购订单结算回写完成: orderId={}, settledQty={}, settledAmount={}",
                order.getId(), order.getSettledQuantity(), order.getSettledAmount());
    }
}