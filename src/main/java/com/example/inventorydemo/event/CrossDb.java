package com.example.inventorydemo.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跨库事件注解，标记在事件类上，自动将事件写入目标模块所在数据库的 EVENT_PUBLICATION 表。
 *
 * 使用方式：
 * <pre>
 * &#64;CrossDb(targetModules = {"settlement"})
 * public record PurchaseOrderCompletedEvent(...) {}
 * </pre>
 *
 * 工作原理：
 * 1. 生产者事务提交后，CrossDbEventProcessor 拦截带有此注解的事件
 * 2. 根据目标模块名从 CrossDbTargetRegistry 获取对应的 JdbcTemplate 和 listenerId
 * 3. 将事件写入目标库的 EVENT_PUBLICATION 表
 * 4. 目标库的 @ApplicationModuleListener 正常消费并归档
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossDb {

    /**
     * 目标模块名称列表，对应 CrossDbTargetRegistry 中注册的模块名
     */
    String[] targetModules();
}
