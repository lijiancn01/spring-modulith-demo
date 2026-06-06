package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 跨库事件消费触发器
 *
 * 跨库事件已写入消费者库的 EVENT_PUBLICATION 表，
 * 此处定时触发 Spring Modulith 的 resubmit 机制，
 * 让未完成的事件重新发布到 Spring 事件总线，
 * 由 @ApplicationModuleListener 消费。
 *
 * 消费成功后，Spring Modulith 自动归档到 EVENT_PUBLICATION_ARCHIVE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossDbEventConsumer {

    private final IncompleteEventPublications incompleteEventPublications;

    @Scheduled(fixedDelay = 3000)
    public void resubmitIncompletePublications() {
        incompleteEventPublications.resubmitIncompletePublications(event -> true);
    }
}
