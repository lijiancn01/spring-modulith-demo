package com.example.inventorydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Modulith
@EnableScheduling
public class InventoryDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryDemoApplication.class, args);
    }
}