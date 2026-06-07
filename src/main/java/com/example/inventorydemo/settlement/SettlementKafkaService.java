package com.example.inventorydemo.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class SettlementKafkaService {

    private final JdbcTemplate settlementJdbcTemplate;
    private final TransactionTemplate settlementTxTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementKafkaService(@Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                                  @Qualifier("settlementTransactionManager") PlatformTransactionManager settlementTransactionManager,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.settlementTxTemplate = new TransactionTemplate(settlementTransactionManager);
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Settlement> getAllSettlements() {
        return settlementJdbcTemplate.query(
                "SELECT * FROM settlements ORDER BY id",
                new BeanPropertyRowMapper<>(Settlement.class));
    }

    public Settlement settle(Long id) {
        settlementTxTemplate.executeWithoutResult(status -> {
            int updated = settlementJdbcTemplate.update(
                    "UPDATE settlements SET status = 'SETTLED' WHERE id = ? AND status = 'PENDING'",
                    id);
            if (updated == 0) {
                throw new RuntimeException("Settlement not found or not in pending status");
            }
        });
        Settlement settlement = settlementJdbcTemplate.queryForObject(
                "SELECT * FROM settlements WHERE id = ?",
                new BeanPropertyRowMapper<>(Settlement.class), id);

        // 结算完成后，通过 Kafka 通知采购/销售模块回写结算量和结算金额
        String topic = settlement.getType() == SettlementType.PAYABLE
                ? "purchase-settlement-completed"
                : "sale-settlement-completed";
        Map<String, Object> event = Map.of(
                "orderId", settlement.getOrderId(),
                "quantity", settlement.getQuantity(),
                "amount", settlement.getAmount(),
                "type", settlement.getType().name()
        );
        kafkaTemplate.send(topic, String.valueOf(settlement.getOrderId()), event);
        log.info("结算完成事件已发送到 Kafka: topic={}, orderId={}, quantity={}, amount={}",
                topic, settlement.getOrderId(), settlement.getQuantity(), settlement.getAmount());

        return settlement;
    }
}
