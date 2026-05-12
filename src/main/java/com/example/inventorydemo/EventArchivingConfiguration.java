package com.example.inventorydemo;

import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.config.EnablePersistentEvents;
import org.springframework.modulith.events.support.CompletedEventPublications;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnablePersistentEvents
public class EventArchivingConfiguration {

    private final CompletedEventPublications eventPublications;

    public EventArchivingConfiguration(CompletedEventPublications eventPublications) {
        this.eventPublications = eventPublications;
    }

    @Scheduled(fixedRate = 60000)
    public void archiveOldEvents() {
        eventPublications.deleteAllCompletedOlderThan(java.time.Duration.ofMinutes(5));
    }
}
