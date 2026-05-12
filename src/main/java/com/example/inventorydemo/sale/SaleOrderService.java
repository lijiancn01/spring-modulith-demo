package com.example.inventorydemo.sale;

import com.example.inventorydemo.inventory.StockDeductedEvent;
import lombok.RequiredArgsConstructor;
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
public class SaleOrderService {
    private final SaleOrderRepository saleOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<SaleOrder> getAllSaleOrders() {
        return saleOrderRepository.findAll();
    }

    public Optional<SaleOrder> getSaleOrderById(Long id) {
        return saleOrderRepository.findById(id);
    }

    @Transactional
    public SaleOrder createSaleOrder(Long productId, int quantity, BigDecimal unitPrice) {
        SaleOrder order = new SaleOrder();
        order.setOrderNumber("SO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(SaleOrderStatus.PENDING);
        return saleOrderRepository.save(order);
    }

    @Transactional
    public SaleOrder completeSaleOrder(Long id) {
        SaleOrder order = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale order not found"));
        if (order.getStatus() != SaleOrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }
        order.setStatus(SaleOrderStatus.COMPLETED);
        eventPublisher.publishEvent(new StockDeductedEvent(order.getProductId(), order.getQuantity()));
        return saleOrderRepository.save(order);
    }

    @Transactional
    public SaleOrder cancelSaleOrder(Long id) {
        SaleOrder order = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale order not found"));
        if (order.getStatus() != SaleOrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }
        order.setStatus(SaleOrderStatus.CANCELLED);
        return saleOrderRepository.save(order);
    }
}