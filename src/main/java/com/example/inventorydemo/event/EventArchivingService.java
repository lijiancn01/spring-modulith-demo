package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventArchivingService {
    private final EventLogRepository eventLogRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void archiveOldEvents() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        int archivedCount = eventLogRepository.archiveOldEvents(fiveMinutesAgo);
        if (archivedCount > 0) {
            log.info("Archived {} events older than {}", archivedCount, fiveMinutesAgo);
        }
    }
}
