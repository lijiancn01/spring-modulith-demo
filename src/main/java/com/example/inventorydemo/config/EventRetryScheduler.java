package com.example.inventorydemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 事件重试调度器
 *
 * 定时扫描未完成的事件发布，重新投递给监听器。
 * 解决 AFTER_COMMIT 后监听器执行失败、运行时异常等场景下的重试问题。
 *
 * 重试策略：
 * - 仅重投递超过 5 秒的未完成事件（避免重投递正在处理中的事件）
 * - 每 10 秒扫描一次
 * - 配合 @Retryable 实现即时重试 + 定时兜底重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRetryScheduler {

    private final IncompleteEventPublications incompleteEventPublications;

    @Scheduled(fixedDelay = 10000)
    public void retryIncompletePublications() {
        try {
            incompleteEventPublications.resubmitIncompletePublicationsOlderThan(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("重投递未完成事件失败: {}", e.getMessage());
        }
    }
}
