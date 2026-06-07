package com.example.inventorydemo.sale;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sale-orders")
@RequiredArgsConstructor
public class SaleOrderController {
    private final SaleOrderService saleOrderService;

    @GetMapping
    public List<SaleOrder> getAllSaleOrders() {
        return saleOrderService.getAllSaleOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleOrder> getSaleOrderById(@PathVariable Long id) {
        return saleOrderService.getSaleOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SaleOrder createSaleOrder(@Valid @RequestBody CreateSaleOrderRequest request) {
        return saleOrderService.createSaleOrder(request.productId(), request.quantity(), request.unitPrice());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SaleOrder> completeSaleOrder(@PathVariable Long id) {
        return ResponseEntity.ok(saleOrderService.completeSaleOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleOrder> cancelSaleOrder(@PathVariable Long id) {
        return ResponseEntity.ok(saleOrderService.cancelSaleOrder(id));
    }
}
