package com.example.inventorydemo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跨库事件消费端：轮询本库的 cross_db_events 表，将事件重新发布到 Spring 事件总线
 *
 * 消费端模块的 @ApplicationModuleListener 会正常接收并处理，
 * 处理成功后标记 cross_db_event 为已处理（同库同事务，保证一致）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossDbEventConsumer {

    private final CrossDbEventRepository crossDbEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    public void pollAndProcess() {
        List<CrossDbEvent> pendingEvents = crossDbEventRepository
                .findByTargetModuleAndProcessedFalseOrderByPublishedAtAsc("settlement");

        for (CrossDbEvent crossDbEvent : pendingEvents) {
            processEvent(crossDbEvent);
        }
    }

    @Transactional
    public void processEvent(CrossDbEvent crossDbEvent) {
        try {
            Class<?> eventClass = Class.forName(crossDbEvent.getEventType());
            Object event = objectMapper.readValue(crossDbEvent.getSerializedEvent(), eventClass);

            // 重新发布到 Spring 事件总线，@ApplicationModuleListener 会接收
            eventPublisher.publishEvent(event);

            // 标记为已处理（与业务操作在同一事务中）
            crossDbEvent.setProcessed(true);
            crossDbEvent.setProcessedAt(LocalDateTime.now());
            crossDbEventRepository.save(crossDbEvent);

            log.info("跨库事件已消费: type={}, id={}", crossDbEvent.getEventType(), crossDbEvent.getId());
        } catch (Exception e) {
            log.error("跨库事件消费失败: id={}, type={}", crossDbEvent.getId(), crossDbEvent.getEventType(), e);
            // 不标记为已处理，下次轮询重试
        }
    }
}
