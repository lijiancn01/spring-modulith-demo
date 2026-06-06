package com.example.inventorydemo.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrossDbEventRepository extends JpaRepository<CrossDbEvent, Long> {
    List<CrossDbEvent> findByTargetModuleAndProcessedFalseOrderByPublishedAtAsc(String targetModule);
    boolean existsByEventTypeAndSerializedEventAndTargetModule(String eventType, String serializedEvent, String targetModule);
}
