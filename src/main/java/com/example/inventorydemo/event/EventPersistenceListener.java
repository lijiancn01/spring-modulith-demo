package com.example.inventorydemo.event;

import com.example.inventorydemo.inventory.StockAddedEvent;
import com.example.inventorydemo.inventory.StockDeductedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPersistenceListener {
    private final EventLogRepository eventLogRepository;

    @EventListener
    @Transactional
    public void handleStockAddedEvent(StockAddedEvent event) {
        log.info("Persistence: Received StockAddedEvent - productId={}, quantity={}", 
                 event.productId(), event.quantity());
        EventLog eventLog = EventLog.fromStockAddedEvent(event.productId(), event.quantity());
        eventLogRepository.save(eventLog);
        log.info("Persistence: StockAddedEvent saved to database with id={}", eventLog.getId());
    }

    @EventListener
    @Transactional
    public void handleStockDeductedEvent(StockDeductedEvent event) {
        log.info("Persistence: Received StockDeductedEvent - productId={}, quantity={}", 
                 event.productId(), event.quantity());
        EventLog eventLog = EventLog.fromStockDeductedEvent(event.productId(), event.quantity());
        eventLogRepository.save(eventLog);
        log.info("Persistence: StockDeductedEvent saved to database with id={}", eventLog.getId());
    }
}
