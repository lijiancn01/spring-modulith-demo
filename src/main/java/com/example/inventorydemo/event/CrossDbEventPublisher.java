package com.example.inventorydemo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 跨库事件发布器：将事件写入目标模块所在的数据库
 *
 * 使用方式：
 *   crossDbEventPublisher.publish(event, "settlement");
 *
 * 调用时机：在生产者事务提交之后（@TransactionalEventListener 或手动调用）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossDbEventPublisher {

    private final CrossDbEventRepository crossDbEventRepository;
    private final ObjectMapper objectMapper;

    public void publish(Object event, String targetModule) {
        publish(event, targetModule, inferSourceModule(event));
    }

    public void publish(Object event, String targetModule, String sourceModule) {
        try {
            String eventType = event.getClass().getName();
            String serializedEvent = objectMapper.writeValueAsString(event);

            // 幂等：如果同类型同内容的事件已存在，跳过
            if (crossDbEventRepository.existsByEventTypeAndSerializedEventAndTargetModule(
                    eventType, serializedEvent, targetModule)) {
                log.info("跨库事件已存在，跳过: type={}, target={}", eventType, targetModule);
                return;
            }

            CrossDbEvent crossDbEvent = new CrossDbEvent();
            crossDbEvent.setEventType(eventType);
            crossDbEvent.setSerializedEvent(serializedEvent);
            crossDbEvent.setSourceModule(sourceModule);
            crossDbEvent.setTargetModule(targetModule);
            crossDbEvent.setPublishedAt(LocalDateTime.now());
            crossDbEvent.setProcessed(false);

            crossDbEventRepository.save(crossDbEvent);
            log.info("跨库事件已写入: type={}, source={}, target={}", eventType, sourceModule, targetModule);
        } catch (Exception e) {
            log.error("跨库事件写入失败: type={}, target={}", event.getClass().getName(), targetModule, e);
            throw new RuntimeException("跨库事件写入失败", e);
        }
    }

    private String inferSourceModule(Object event) {
        String pkg = event.getClass().getPackageName();
        String[] parts = pkg.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }
}
