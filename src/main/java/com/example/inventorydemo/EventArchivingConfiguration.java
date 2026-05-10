package com.example.inventorydemo;

import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.config.EnablePersistentEvents;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import org.springframework.modulith.events.support.CompletedEventPublications;

@Configuration
@EnablePersistentEvents
@EnableScheduling
public class EventArchivingConfiguration {

    private final CompletedEventPublications completedEventPublications;

    public EventArchivingConfiguration(CompletedEventPublications completedEventPublications) {
        this.completedEventPublications = completedEventPublications;
    }

    @Scheduled(fixedRate = 60000)
    public void archiveCompletedEvents() {
        completedEventPublications.deleteCompletedOlderThan(Duration.ofMinutes(5));
    }
}
