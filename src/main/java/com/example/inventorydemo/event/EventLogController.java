package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventLogController {
    private final EventLogRepository eventLogRepository;

    @GetMapping
    public List<EventLog> getAllEvents() {
        return eventLogRepository.findByArchivedFalseOrderByCreatedAtDesc();
    }

    @GetMapping("/all")
    public List<EventLog> getAllEventsIncludingArchived() {
        return eventLogRepository.findAll();
    }

    @GetMapping("/product/{productId}")
    public List<EventLog> getEventsByProductId(@PathVariable Long productId) {
        return eventLogRepository.findByProductId(productId);
    }
}
