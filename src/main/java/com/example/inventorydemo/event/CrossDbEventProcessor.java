package com.example.inventorydemo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 跨库事件处理器：自动拦截带有 @CrossDb 注解的事件，写入目标库的 EVENT_PUBLICATION 表
 *
 * 工作流程：
 * 1. 生产者事务提交后（AFTER_COMMIT），此处理器拦截事件
 * 2. 检查事件类是否有 @CrossDb 注解
 * 3. 如果有，遍历 targetModules，从 CrossDbTargetRegistry 获取目标 JdbcTemplate
 * 4. 将事件写入目标库的 EVENT_PUBLICATION 表
 * 5. 目标库的 CrossDbEventConsumer 定时触发 resubmit，@ApplicationModuleListener 消费并归档
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossDbEventProcessor {

    private final CrossDbTargetRegistry targetRegistry;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseCompleted(PurchaseOrderCompletedEvent event) {
        processCrossDbEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(SaleOrderCompletedEvent event) {
        processCrossDbEvent(event);
    }

    private void processCrossDbEvent(Object event) {
        CrossDb crossDb = event.getClass().getAnnotation(CrossDb.class);
        if (crossDb == null) {
            return;
        }

        String eventType = event.getClass().getName();
        for (String targetModule : crossDb.targetModules()) {
            if (!targetRegistry.isRegistered(targetModule)) {
                log.warn("跨库目标模块未注册，跳过: module={}, event={}", targetModule, eventType);
                continue;
            }

            try {
                publishToTarget(event, targetModule);
            } catch (Exception e) {
                log.error("跨库事件写入失败: target={}, event={}", targetModule, eventType, e);
            }
        }
    }

    private void publishToTarget(Object event, String targetModule) throws Exception {
        String eventType = event.getClass().getName();
        String serializedEvent = objectMapper.writeValueAsString(event);
        String listenerId = targetRegistry.getListenerId(targetModule, eventType);

        if (listenerId == null) {
            log.error("目标模块未配置事件对应的 listenerId: module={}, eventType={}", targetModule, eventType);
            return;
        }

        var targetJdbc = targetRegistry.getJdbcTemplate(targetModule);

        // 幂等：检查目标库是否已存在相同事件
        Integer count = targetJdbc.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE = ? AND LISTENER_ID = ? AND SERIALIZED_EVENT = ?",
                Integer.class, eventType, listenerId, serializedEvent);
        if (count != null && count > 0) {
            log.info("跨库事件已存在于目标库，跳过: type={}, target={}", eventType, targetModule);
            return;
        }

        UUID eventId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        targetJdbc.update(
                "INSERT INTO EVENT_PUBLICATION (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, COMPLETION_DATE) " +
                "VALUES (?, ?, ?, ?, ?, NULL)",
                eventId, eventType, listenerId, now, serializedEvent);

        log.info("跨库事件已写入目标库: id={}, type={}, target={}, listener={}",
                eventId, eventType, targetModule, listenerId);
    }
}
