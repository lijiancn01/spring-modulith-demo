package com.example.inventorydemo.inventory;

import com.example.inventorydemo.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Transactional
    public Inventory addStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory inv = new Inventory();
                    inv.setProductId(productId);
                    inv.setQuantity(0);
                    return inv;
                });
        inventory.setQuantity(inventory.getQuantity() + quantity);
        try {
            return inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("乐观锁冲突，重试 addStock: productId={}", productId);
            return addStockRetry(productId, quantity);
        }
    }

    private Inventory addStockRetry(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("库存记录不存在: productId=" + productId));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory deductStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("库存记录不存在: productId=" + productId));
        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException("库存不足: 当前=" + inventory.getQuantity() + ", 需要=" + quantity);
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        return inventoryRepository.save(inventory);
    }
}