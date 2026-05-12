package com.example.inventorydemo.purchase;

import com.example.inventorydemo.inventory.StockAddedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(PurchaseOrderStatus.PENDING);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder completePurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }
        order.setStatus(PurchaseOrderStatus.COMPLETED);
        eventPublisher.publishEvent(new StockAddedEvent(order.getProductId(), order.getQuantity()));
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }
}