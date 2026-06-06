package com.example.inventorydemo.event;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import com.example.inventorydemo.settlement.SettlementService;
import com.example.inventorydemo.settlement.SettlementType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 跨库事件消费触发器
 *
 * 双重机制：
 * 1. 主库 resubmit：处理主库 EVENT_PUBLICATION 中未完成的事件（库存模块等）
 * 2. 结算库轮询：扫描结算库 EVENT_PUBLICATION 中未完成的事件，
 *    反序列化后直接调用结算服务处理，避免重新发布到事件总线导致循环消费。
 *    处理成功后手动标记结算库事件完成。
 */
@Slf4j
@Component
public class CrossDbEventConsumer {

    private final IncompleteEventPublications incompleteEventPublications;
    private final JdbcTemplate settlementJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SettlementService settlementService;

    public CrossDbEventConsumer(IncompleteEventPublications incompleteEventPublications,
                                @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate,
                                ObjectMapper objectMapper,
                                SettlementService settlementService) {
        this.incompleteEventPublications = incompleteEventPublications;
        this.settlementJdbcTemplate = settlementJdbcTemplate;
        this.objectMapper = objectMapper;
        this.settlementService = settlementService;
        try {
            String url = settlementJdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            log.info("CrossDbEventConsumer settlementJdbcTemplate URL: {}", url);
        } catch (Exception e) {
            log.error("Failed to get URL", e);
        }
    }

    @Scheduled(fixedDelay = 3000)
    public void resubmitIncompletePublications() {
        // 1. 主库 resubmit（处理库存模块等本地消费者）
        incompleteEventPublications.resubmitIncompletePublications(event -> true);

        // 2. 结算库轮询：扫描未完成事件并直接调用结算服务
        processSettlementIncompleteEvents();
    }

    private void processSettlementIncompleteEvents() {
        List<Map<String, Object>> incompleteEvents = settlementJdbcTemplate.queryForList(
                "SELECT ID, EVENT_TYPE, SERIALIZED_EVENT FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL");

        log.info("结算库未完成事件数: {}", incompleteEvents.size());

        for (Map<String, Object> row : incompleteEvents) {
            String eventId = row.get("ID").toString();
            String eventType = (String) row.get("EVENT_TYPE");
            String serializedEvent = (String) row.get("SERIALIZED_EVENT");

            try {
                boolean success = handleSettlementEvent(eventType, serializedEvent);
                log.info("结算库事件处理结果: id={}, type={}, success={}", eventId, eventType, success);
                if (success) {
                    // 标记结算库事件完成
                    settlementJdbcTemplate.update(
                            "UPDATE EVENT_PUBLICATION SET COMPLETION_DATE = ? WHERE ID = ?",
                            OffsetDateTime.now(), UUID.fromString(eventId));
                    log.info("结算库事件消费成功: id={}, type={}", eventId, eventType);
                }
            } catch (Exception e) {
                log.error("结算库事件消费失败: id={}, type={}", eventId, eventType, e);
            }
        }
    }

    private boolean handleSettlementEvent(String eventType, String serializedEvent) throws Exception {
        if (PurchaseOrderCompletedEvent.class.getName().equals(eventType)) {
            PurchaseOrderCompletedEvent event = objectMapper.readValue(serializedEvent, PurchaseOrderCompletedEvent.class);
            settlementService.createSettlement(event.orderId(), SettlementType.PAYABLE, event.totalAmount());
            return true;
        } else if (SaleOrderCompletedEvent.class.getName().equals(eventType)) {
            SaleOrderCompletedEvent event = objectMapper.readValue(serializedEvent, SaleOrderCompletedEvent.class);
            settlementService.createSettlement(event.orderId(), SettlementType.RECEIVABLE, event.totalAmount());
            return true;
        }
        log.warn("结算库事件类型未处理: type={}", eventType);
        return false;
    }
}
