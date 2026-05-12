package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.EventPublicationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventPublicationController {

    private final EventPublicationRepository eventPublicationRepository;

    @GetMapping
    public List<EventPublication> getAllEvents() {
        return eventPublicationRepository.findAll();
    }

    @GetMapping("/incomplete")
    public List<EventPublication> getIncompleteEvents() {
        return eventPublicationRepository.findIncompletePublications();
    }

    @GetMapping("/completed")
    public List<EventPublication> getCompletedEvents() {
        return eventPublicationRepository.findAll().stream()
                .filter(EventPublication::isCompletionSignalled)
                .toList();
    }
}
