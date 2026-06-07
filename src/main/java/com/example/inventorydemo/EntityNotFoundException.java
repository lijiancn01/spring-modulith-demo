package com.example.inventorydemo;

/**
 * 实体未找到异常
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
