package com.example.inventorydemo.settlement;

import com.example.inventorydemo.EntityNotFoundException;
import com.example.inventorydemo.InvalidStatusException;
import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(value = "settlementTransactionManager", readOnly = true)
    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    @Transactional(value = "settlementTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void createFromPurchase(PurchaseOrderCompletedEvent event) {
        try {
            Settlement settlement = new Settlement();
            settlement.setOrderId(event.orderId());
            settlement.setType(SettlementType.PAYABLE);
            settlement.setQuantity(event.quantity());
            settlement.setAmount(event.totalAmount());
            settlement.setStatus(SettlementStatus.PENDING);
            settlementRepository.save(settlement);
            log.info("采购结算记录已保存: id={}, orderId={}", settlement.getId(), settlement.getOrderId());
        } catch (DataIntegrityViolationException e) {
            log.info("结算记录已存在，跳过: orderId={}, type=PAYABLE", event.orderId());
        }
    }

    @Transactional(value = "settlementTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void createFromSale(SaleOrderCompletedEvent event) {
        try {
            Settlement settlement = new Settlement();
            settlement.setOrderId(event.orderId());
            settlement.setType(SettlementType.RECEIVABLE);
            settlement.setQuantity(event.quantity());
            settlement.setAmount(event.totalAmount());
            settlement.setStatus(SettlementStatus.PENDING);
            settlementRepository.save(settlement);
            log.info("销售结算记录已保存: id={}, orderId={}", settlement.getId(), settlement.getOrderId());
        } catch (DataIntegrityViolationException e) {
            log.info("结算记录已存在，跳过: orderId={}, type=RECEIVABLE", event.orderId());
        }
    }

    @Transactional("settlementTransactionManager")
    public Settlement settle(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("结算记录不存在: " + id));
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new InvalidStatusException("结算记录状态不是待结算: " + id);
        }
        settlement.setStatus(SettlementStatus.SETTLED);

        eventPublisher.publishEvent(new SettlementCompletedEvent(
                settlement.getOrderId(),
                settlement.getQuantity(),
                settlement.getAmount(),
                settlement.getType()
        ));
        log.info("结算完成事件已发布: orderId={}, quantity={}, amount={}, type={}",
                settlement.getOrderId(), settlement.getQuantity(),
                settlement.getAmount(), settlement.getType());

        return settlement;
    }
}
