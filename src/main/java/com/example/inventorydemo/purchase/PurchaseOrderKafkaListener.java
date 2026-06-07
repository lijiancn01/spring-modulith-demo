package com.example.inventorydemo.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 采购模块 Kafka 事件监听器（Kafka 模式）
 *
 * 监听结算模块的结算完成事件，回写结算量和结算金额到采购单。
 * 使用 @RetryableTopic 实现自动重试，失败后进入 DLT。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class PurchaseOrderKafkaListener {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.MULTIPLE_TOPICS,
            retryTopicSuffix = "-retry", dltTopicSuffix = "-dlt",
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "purchase-settlement-completed", groupId = "purchase-group")
    public void handleSettlementCompleted(Map<String, Object> event,
                                          @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attempts) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        int quantity = ((Number) event.get("quantity")).intValue();
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        log.info("采购模块收到结算完成事件（Kafka）: orderId={}, quantity={}, amount={}, attempts={}", orderId, quantity, amount, attempts);

        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found: " + orderId));
        order.setSettledQuantity(order.getSettledQuantity() + quantity);
        order.setSettledAmount(order.getSettledAmount().add(amount));
        purchaseOrderRepository.save(order);
    }
}
