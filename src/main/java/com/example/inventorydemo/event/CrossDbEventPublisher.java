package com.example.inventorydemo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 跨库事件发布器：将事件写入消费者所在数据库的 EVENT_PUBLICATION 表
 *
 * 消费者库也有 Spring Modulith 的 EVENT_PUBLICATION 表，
 * 写入后消费者用原生 @ApplicationModuleListener 消费，
 * 消费成功后自动归档到 EVENT_PUBLICATION_ARCHIVE。
 *
 * 使用方式：
 *   crossDbEventPublisher.publish(event, "settlement", settlementJdbcTemplate);
 *
 * 调用时机：在生产者事务提交之后（@TransactionalEventListener(AFTER_COMMIT)）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossDbEventPublisher {

    private final ObjectMapper objectMapper;

    /**
     * 将事件写入目标数据库的 EVENT_PUBLICATION 表
     *
     * @param event           事件对象
     * @param listenerId      目标监听器的完整标识，如 "com.example.inventorydemo.settlement.SettlementEventListener.handlePurchaseCompleted"
     * @param targetJdbcTemplate 目标数据库的 JdbcTemplate
     */
    public void publish(Object event, String listenerId, JdbcTemplate targetJdbcTemplate) {
        try {
            String eventType = event.getClass().getName();
            String serializedEvent = objectMapper.writeValueAsString(event);
            UUID eventId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();

            // 幂等：检查目标库是否已存在相同事件
            Integer count = targetJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE EVENT_TYPE = ? AND LISTENER_ID = ? AND SERIALIZED_EVENT = ?",
                    Integer.class, eventType, listenerId, serializedEvent);
            if (count != null && count > 0) {
                log.info("跨库事件已存在于目标库，跳过: type={}, listener={}", eventType, listenerId);
                return;
            }

            targetJdbcTemplate.update(
                    "INSERT INTO EVENT_PUBLICATION (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, COMPLETION_DATE) " +
                    "VALUES (?, ?, ?, ?, ?, NULL)",
                    eventId, eventType, listenerId, now, serializedEvent);

            log.info("跨库事件已写入目标库 EVENT_PUBLICATION: id={}, type={}, listener={}", eventId, eventType, listenerId);
        } catch (Exception e) {
            log.error("跨库事件写入目标库失败: type={}, listener={}", event.getClass().getName(), listenerId, e);
            throw new RuntimeException("跨库事件写入失败", e);
        }
    }
}
