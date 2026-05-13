package com.example.inventorydemo.event;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventPublicationController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/archive/count")
    public Map<String, Object> getArchiveCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION_ARCHIVE", Integer.class);
        return Map.of("count", count != null ? count : 0);
    }

    @GetMapping("/archive")
    public List<Map<String, Object>> getArchiveList() {
        return jdbcTemplate.queryForList(
                "SELECT ID, EVENT_TYPE, LISTENER_ID, COMPLETION_DATE, PUBLICATION_DATE FROM EVENT_PUBLICATION_ARCHIVE");
    }

    @GetMapping("/publication/count")
    public Map<String, Object> getPublicationCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION", Integer.class);
        return Map.of("count", count != null ? count : 0);
    }
}
