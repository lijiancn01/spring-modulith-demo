package com.example.inventorydemo.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByArchivedFalseOrderByCreatedAtDesc();
    
    List<EventLog> findByProductId(Long productId);
    
    @Modifying
    @Query("UPDATE EventLog e SET e.archived = true WHERE e.createdAt < :beforeTime AND e.archived = false")
    int archiveOldEvents(@Param("beforeTime") LocalDateTime beforeTime);
}
