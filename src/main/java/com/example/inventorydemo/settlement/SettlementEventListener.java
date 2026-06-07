package com.example.inventorydemo.settlement;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 结算模块事件监听器
 *
 * 监听采购/销售完结事件，写入结算库（跨数据库）。
 * 幂等保护：通过 existsByOrderIdAndType 检查防止重复创建结算记录。
 * 使用 JdbcTemplate + 编程式事务管理，因为 @ApplicationModuleListener 内部使用
 * @TransactionalEventListener，不能与 @Transactional 组合使用。
 */
@Slf4j
@Component
public class SettlementEventListener {

    private final SettlementRepository settlementRepository;
    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;

    public SettlementEventListener(SettlementRepository settlementRepository,
                                   @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                                   @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager) {
        this.settlementRepository = settlementRepository;
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
    }

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
        log.info("结算模块收到采购完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());

        settlementTxTemplate.executeWithoutResult(status -> {
            if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.PAYABLE)) {
                log.info("结算记录已存在，跳过: orderId={}, type=PAYABLE", event.orderId());
                return;
            }

            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                    event.orderId(), SettlementType.PAYABLE.name(), event.quantity(), event.totalAmount(), SettlementStatus.PENDING.name());
            log.info("采购结算记录已保存: orderId={}", event.orderId());
        });
    }

    @ApplicationModuleListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleSaleCompleted(SaleOrderCompletedEvent event) {
        log.info("结算模块收到销售完结事件: orderId={}, quantity={}, amount={}",
                event.orderId(), event.quantity(), event.totalAmount());

        settlementTxTemplate.executeWithoutResult(status -> {
            if (settlementRepository.existsByOrderIdAndType(event.orderId(), SettlementType.RECEIVABLE)) {
                log.info("结算记录已存在，跳过: orderId={}, type=RECEIVABLE", event.orderId());
                return;
            }

            settlementJdbcTemplate.update(
                    "INSERT INTO settlements (order_id, type, quantity, amount, status, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                    event.orderId(), SettlementType.RECEIVABLE.name(), event.quantity(), event.totalAmount(), SettlementStatus.PENDING.name());
            log.info("销售结算记录已保存: orderId={}", event.orderId());
        });
    }
}
