package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final CompletedEventPublications completedEventPublications;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/archive/count")
    public Map<String, Object> getArchiveCount() {
        return Map.of("count", completedEventPublications.findAll().size());
    }

    @GetMapping("/publications")
    public List<Map<String, Object>> getEventPublications() {
        return jdbcTemplate.queryForList(
                "SELECT ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, COMPLETION_DATE FROM EVENT_PUBLICATION ORDER BY PUBLICATION_DATE");
    }

    @GetMapping("/archive")
    public List<Map<String, Object>> getEventArchive() {
        return jdbcTemplate.queryForList(
                "SELECT ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, COMPLETION_DATE FROM EVENT_PUBLICATION_ARCHIVE ORDER BY PUBLICATION_DATE");
    }

    @GetMapping("/incomplete/count")
    public Map<String, Object> getIncompleteCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL", Integer.class);
        return Map.of("count", count != null ? count : 0);
    }
}
