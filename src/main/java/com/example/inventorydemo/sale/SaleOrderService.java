package com.example.inventorydemo.sale;

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
        order.setStatus(SaleOrderStatus.PENDING);
        return saleOrderRepository.save(order);
    }

    @Transactional
    public SaleOrder completeSaleOrder(Long id) {
        SaleOrder order = saleOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("销售订单不存在: " + id));
        if (order.getStatus() != SaleOrderStatus.PENDING) {
            throw new InvalidStatusException("销售订单状态不是待处理: " + id);
        }
        order.setStatus(SaleOrderStatus.COMPLETED);
        eventPublisher.publishEvent(new SaleOrderCompletedEvent(
                order.getId(), order.getProductId(), order.getQuantity(),
                order.getUnitPrice(), order.getTotalAmount()));
        return saleOrderRepository.save(order);
    }

    @Transactional
    public SaleOrder cancelSaleOrder(Long id) {
        SaleOrder order = saleOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("销售订单不存在: " + id));
        if (order.getStatus() != SaleOrderStatus.PENDING) {
            throw new InvalidStatusException("销售订单状态不是待处理: " + id);
        }
        order.setStatus(SaleOrderStatus.CANCELLED);
        return saleOrderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySettlement(SettlementCompletedEvent event) {
        if (event.type() != SettlementType.RECEIVABLE) {
            return;
        }
        SaleOrder order = saleOrderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("销售订单不存在: " + event.orderId()));
        if (order.getSettledQuantity() > 0) {
            return;
        }
        order.setSettledQuantity(event.quantity());
        order.setSettledAmount(event.amount());
        saleOrderRepository.save(order);
        log.info("销售订单结算回写完成: orderId={}, settledQty={}, settledAmount={}",
                order.getId(), order.getSettledQuantity(), order.getSettledAmount());
    }
}