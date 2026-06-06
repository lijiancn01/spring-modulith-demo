package com.example.inventorydemo.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 跨库事件表，建在消费者所在的数据库中
 */
@Entity
@Table(name = "cross_db_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrossDbEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "serialized_event", nullable = false, length = 4000)
    private String serializedEvent;

    @Column(name = "source_module", nullable = false)
    private String sourceModule;

    @Column(name = "target_module", nullable = false)
    private String targetModule;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
