package com.example.inventorydemo;

/**
 * 状态不合法异常
 */
public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String message) {
        super(message);
    }
}
