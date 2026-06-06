package com.example.inventorydemo.event;

import com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent;
import com.example.inventorydemo.sale.SaleOrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

/**
 * 跨库事件目标注册配置
 *
 * 将目标模块名映射到对应的 JdbcTemplate，
 * 并按事件类型注册对应的 listenerId。
 *
 * 同时在目标库中初始化 EVENT_PUBLICATION 和 EVENT_PUBLICATION_ARCHIVE 表，
 * 因为目标库不是 Spring Modulith 的主数据源，不会自动创建这些表。
 */
@Slf4j
@Configuration
public class CrossDbTargetConfig {

    private final CrossDbTargetRegistry targetRegistry;
    private final JdbcTemplate settlementJdbcTemplate;

    public CrossDbTargetConfig(CrossDbTargetRegistry targetRegistry,
                               @Qualifier("settlementJdbcTemplate") JdbcTemplate settlementJdbcTemplate) {
        this.targetRegistry = targetRegistry;
        this.settlementJdbcTemplate = settlementJdbcTemplate;
    }

    @PostConstruct
    public void registerTargets() {
        // 在结算库中初始化事件表
        initSettlementEventTables();

        targetRegistry.register("settlement", settlementJdbcTemplate);
        targetRegistry.registerListenerId("settlement",
                PurchaseOrderCompletedEvent.class.getName(),
                "com.example.inventorydemo.settlement.SettlementEventListener.handlePurchaseCompleted(com.example.inventorydemo.purchase.PurchaseOrderCompletedEvent)");
        targetRegistry.registerListenerId("settlement",
                SaleOrderCompletedEvent.class.getName(),
                "com.example.inventorydemo.settlement.SettlementEventListener.handleSaleCompleted(com.example.inventorydemo.sale.SaleOrderCompletedEvent)");
    }

    private void initSettlementEventTables() {
        settlementJdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION (
                ID UUID NOT NULL,
                COMPLETION_DATE TIMESTAMP(9) WITH TIME ZONE,
                EVENT_TYPE VARCHAR(512) NOT NULL,
                LISTENER_ID VARCHAR(512) NOT NULL,
                PUBLICATION_DATE TIMESTAMP(9) WITH TIME ZONE NOT NULL,
                SERIALIZED_EVENT VARCHAR(4000) NOT NULL,
                PRIMARY KEY (ID)
            )
        """);
        settlementJdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS EVENT_PUBLICATION_BY_LISTENER_ID_AND_SERIALIZED_EVENT_IDX
            ON EVENT_PUBLICATION (LISTENER_ID, SERIALIZED_EVENT)
        """);
        settlementJdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX
            ON EVENT_PUBLICATION (COMPLETION_DATE)
        """);

        settlementJdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION_ARCHIVE (
                ID UUID NOT NULL,
                COMPLETION_DATE TIMESTAMP(9) WITH TIME ZONE,
                EVENT_TYPE VARCHAR(512) NOT NULL,
                LISTENER_ID VARCHAR(512) NOT NULL,
                PUBLICATION_DATE TIMESTAMP(9) WITH TIME ZONE NOT NULL,
                SERIALIZED_EVENT VARCHAR(4000) NOT NULL,
                PRIMARY KEY (ID)
            )
        """);
        settlementJdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS EVENT_PUBLICATION_ARCHIVE_BY_LISTENER_ID_AND_SERIALIZED_EVENT_IDX
            ON EVENT_PUBLICATION_ARCHIVE (LISTENER_ID, SERIALIZED_EVENT)
        """);
        settlementJdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS EVENT_PUBLICATION_ARCHIVE_BY_COMPLETION_DATE_IDX
            ON EVENT_PUBLICATION_ARCHIVE (COMPLETION_DATE)
        """);

        log.info("结算库事件表初始化完成");
    }
}
