package com.example.inventorydemo.event;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨库事件目标注册表：模块名 → JdbcTemplate 映射
 *
 * 跨库场景下，每个目标模块对应一个独立的数据源，
 * 通过此注册表将模块名映射到对应的 JdbcTemplate。
 * listenerId 按事件类型分别注册，因为同一模块可能有多个监听方法。
 */
@Component
public class CrossDbTargetRegistry {

    private final Map<String, JdbcTemplate> targets = new ConcurrentHashMap<>();
    // Key: "moduleName:eventTypeName" -> listenerId
    private final Map<String, String> listenerIds = new ConcurrentHashMap<>();

    /**
     * 注册目标模块
     */
    public void register(String moduleName, JdbcTemplate jdbcTemplate) {
        targets.put(moduleName, jdbcTemplate);
    }

    /**
     * 注册目标模块的监听器ID（按事件类型）
     *
     * @param moduleName    模块名，与 @CrossDb 注解中的 targetModules 对应
     * @param eventTypeName 事件类全限定名
     * @param listenerId    写入 EVENT_PUBLICATION 表时的 LISTENER_ID（含方法参数类型）
     */
    public void registerListenerId(String moduleName, String eventTypeName, String listenerId) {
        listenerIds.put(moduleName + ":" + eventTypeName, listenerId);
    }

    public JdbcTemplate getJdbcTemplate(String moduleName) {
        JdbcTemplate jt = targets.get(moduleName);
        if (jt == null) {
            throw new IllegalArgumentException("未注册的跨库目标模块: " + moduleName);
        }
        return jt;
    }

    public String getListenerId(String moduleName, String eventTypeName) {
        return listenerIds.get(moduleName + ":" + eventTypeName);
    }

    public boolean isRegistered(String moduleName) {
        return targets.containsKey(moduleName);
    }
}
