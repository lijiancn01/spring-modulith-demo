package com.example.inventorydemo.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived")
    private boolean archived = false;

    public static EventLog fromStockAddedEvent(Long productId, int quantity) {
        EventLog log = new EventLog();
        log.setEventType("StockAddedEvent");
        log.setProductId(productId);
        log.setQuantity(quantity);
        log.setEventData("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    public static EventLog fromStockDeductedEvent(Long productId, int quantity) {
        EventLog log = new EventLog();
        log.setEventType("StockDeductedEvent");
        log.setProductId(productId);
        log.setQuantity(quantity);
        log.setEventData("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
