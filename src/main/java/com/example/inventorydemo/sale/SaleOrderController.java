package com.example.inventorydemo.sale;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public SaleOrder createSaleOrder(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        int quantity = Integer.parseInt(request.get("quantity").toString());
        BigDecimal unitPrice = new BigDecimal(request.get("unitPrice").toString());
        return saleOrderService.createSaleOrder(productId, quantity, unitPrice);
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
