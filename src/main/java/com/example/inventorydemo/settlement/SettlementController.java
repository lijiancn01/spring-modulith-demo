package com.example.inventorydemo.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public List<Settlement> getAllSettlements() {
        return settlementService.getAllSettlements();
    }

    @PostMapping("/{id}/settle")
    public Settlement settle(@PathVariable Long id) {
        return settlementService.settle(id);
    }
}
