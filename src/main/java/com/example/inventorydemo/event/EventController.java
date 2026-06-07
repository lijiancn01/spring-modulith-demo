package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final CompletedEventPublications completedEventPublications;

    @GetMapping("/archive/count")
    public Map<String, Object> getArchiveCount() {
        return Map.of("count", completedEventPublications.findAll().size());
    }
}
