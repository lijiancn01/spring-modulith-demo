package com.example.inventorydemo.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByOrderIdAndType(Long orderId, SettlementType type);
}
