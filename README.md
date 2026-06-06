# 库存管理系统 - Spring Boot + Modulith

基于 Spring Boot 3.5.14 和 Spring Modulith 1.4.11 构建的库存管理系统演示项目。

## 项目特性

- 商品管理模块
- 库存管理模块
- 采购管理模块（采购单增加库存）
- 销售管理模块（销售单扣减库存）
- 基于 Spring Modulith 的事件驱动架构
- JDBC 持久化事件存储
- 事件归档支持
- 使用 Spring Modulith 原生功能，无需自定义事件持久化代码

## 技术栈

- Spring Boot 3.5.14
- Spring Modulith 1.4.11
- Spring Data JPA
- H2 内存数据库（默认）/ MySQL
- Lombok

## 项目结构

```
src/main/java/com/example/inventorydemo/
├── InventoryDemoApplication.java          # 主应用类
├── DataInitializer.java                   # 数据初始化
├── event/                                 # 事件监控
│   └── EventController.java
├── product/                               # 商品模块
│   ├── Product.java
│   ├── ProductRepository.java
│   ├── ProductService.java
│   └── ProductController.java
├── inventory/                             # 库存模块
│   ├── Inventory.java
│   ├── InventoryRepository.java
│   ├── InventoryService.java
│   ├── InventoryEventListener.java
│   ├── StockAddedEvent.java
│   └── StockDeductedEvent.java
├── purchase/                              # 采购模块
│   ├── PurchaseOrder.java
│   ├── PurchaseOrderStatus.java
│   ├── PurchaseOrderRepository.java
│   ├── PurchaseOrderService.java
│   └── PurchaseOrderController.java
└── sale/                                  # 销售模块
    ├── SaleOrder.java
    ├── SaleOrderStatus.java
    ├── SaleOrderRepository.java
    ├── SaleOrderService.java
    └── SaleOrderController.java
```

## 数据库配置

默认使用 H2 内存数据库，启动时会自动创建表结构。

### 使用 MySQL（可选）

修改 `application.properties` 中的数据库连接信息：

```properties
# 注释 H2 配置，启用 MySQL 配置
# spring.datasource.url=jdbc:h2:mem:inventorydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
# spring.datasource.driverClassName=org.h2.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_demo
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

## 运行项目

1. 确保已安装 JDK 17 和 Maven
2. 运行以下命令：

```bash
mvn spring-boot:run
```

或者先编译再运行：

```bash
mvn clean package
java -jar target/inventory-demo-0.0.1-SNAPSHOT.jar
```

## 访问应用

启动成功后，在浏览器中访问：

- 主界面：http://localhost:8080/
- H2 控制台：http://localhost:8080/h2-console
- API 端点：
  - 商品：http://localhost:8080/api/products
  - 库存：http://localhost:8080/api/inventory
  - 采购单：http://localhost:8080/api/purchase-orders
  - 销售单：http://localhost:8080/api/sale-orders
  - 事件监控：http://localhost:8080/api/events/archive

## 使用流程

1. **查看商品**：系统会自动初始化 3 个示例商品
2. **创建采购单**：在采购管理页面创建采购单，指定商品、数量和单价
3. **完成采购**：点击采购单的"完成"按钮，库存会自动增加
4. **创建销售单**：在销售管理页面创建销售单
5. **完成销售**：点击销售单的"完成"按钮，库存会自动减少（库存不足时会报错）
6. **查看事件归档**：在事件监控页面查看所有已归档的事件

## 事件机制

项目完全使用 Spring Modulith 的原生事件机制：

- 采购单完成时发布 `StockAddedEvent`，库存模块监听并增加库存
- 销售单完成时发布 `StockDeductedEvent`，库存模块监听并减少库存
- 事件通过 JDBC 持久化存储
- 配置 `spring.modulith.events.completion-mode=archive` 启用事件归档

### 一个事件被多个模块消费的设计

当新增模块（如结算模块）也需要消费采购完成事件时，**不需要为每个消费方写单独的事件**，直接让多个模块监听同一个事件即可。

**核心原则**：事件语义应描述"业务事实"（如采购完成），而非"消费方的动作"（如库存增加）。

```
采购模块发布 PurchaseOrderCompletedEvent
         ↓
    ┌────┴────┐
    ↓         ↓
 库存模块    结算模块
 (监听)     (监听)
```

Spring Modulith 在 `EVENT_PUBLICATION` 表中为每个监听器生成独立记录，消费状态互不影响：

| ID | EVENT_TYPE | LISTENER_ID | COMPLETION_DATE |
|----|-----------|-------------|-----------------|
| xxx | PurchaseOrderCompletedEvent | InventoryEventListener.handlePurchaseCompleted | 10:00:01 |
| yyy | PurchaseOrderCompletedEvent | SettlementEventListener.handlePurchaseCompleted | **null** |

即使库存模块消费成功、结算模块消费失败，重启后也只会重放结算模块那条。

**代码示例**：

```java
// 1. 定义通用领域事件（放在发布方模块）
public record PurchaseOrderCompletedEvent(
    Long orderId, Long productId, int quantity,
    BigDecimal unitPrice, BigDecimal totalAmount
) {}

// 2. 发布方：只发布一个事件
eventPublisher.publishEvent(new PurchaseOrderCompletedEvent(...));

// 3. 库存模块监听
@ApplicationModuleListener
public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
    inventoryService.addStock(event.productId(), event.quantity());
}

// 4. 结算模块监听
@ApplicationModuleListener
public void handlePurchaseCompleted(PurchaseOrderCompletedEvent event) {
    settlementService.createPayable(event.orderId(), event.totalAmount());
}
```

**优势**：
- 新增消费方零改动：只需新增监听器，发布方完全不用改
- 消费状态独立跟踪：某个监听器失败不影响其他监听器
- 事件语义清晰：描述业务事实而非实现细节

## 关键配置说明

```properties
# 启用事件持久化表结构自动初始化
spring.modulith.events.jdbc.schema-initialization.enabled=true

# 事件完成模式为 archive（归档）
spring.modulith.events.completion-mode=archive

# 重启时重新发布未完成的事件
spring.modulith.events.republish-outstanding-events-on-restart=true
```

> **注意**：属性名是 `republish-outstanding-events-on-restart`，不是 `republish-outstanding-on-restart`，少了 `events` 会导致配置不生效。

## 注意事项

- 默认使用 H2 内存数据库，重启后数据会丢失
- 如需持久化数据，配置使用 MySQL
- 销售操作会检查库存是否充足，不足时会报错

---

## 常见问题与解决方案（踩坑记录）

### 1. Maven 依赖下载超时

**问题**：无法下载maven依赖。

**解决方案**：
- 使用 Maven Central 官方仓库，不用使用国内镜像（阿里云，华为云等都不要使用）。
- 配置 Maven 代理（如公司内网代理）
- 修改 `~/.m2/settings.xml` 配置：
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <proxies>
    <proxy>
      <id>proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>127.0.0.1</host>
      <port>18080</port>
      <nonProxyHosts>localhost|127.0.0.1|*.local</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

### 2. Lombok 注解处理器不工作

**问题**：编译时报错 `cannot find symbol method getXXX()` 或 `setXXX()`。

**解决方案**：
- 在 `pom.xml` 中配置 `maven-compiler-plugin` 的注解处理器路径
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3. 事件表（EVENT_PUBLICATION）没有自动创建

**问题**：启动后数据库中没有 `EVENT_PUBLICATION` 和 `EVENT_PUBLICATION_ARCHIVE` 表。

**解决方案**：

**必须添加以下依赖**（缺一不可）：
```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-jackson</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

**必须配置以下内容**：
```properties
# 启用事件持久化表结构自动初始化
spring.modulith.events.jdbc.schema-initialization.enabled=true

# 事件完成模式为 archive（归档）
spring.modulith.events.completion-mode=archive

# H2 数据库需设置 MODE=MySQL
spring.datasource.url=jdbc:h2:mem:inventorydb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

### 4. 事件重启后未重放（属性名写错）

**问题**：配置了 `republish-outstanding-on-restart=true`，但项目重启后 `EVENT_PUBLICATION` 表中未消费的事件仍然没有被重新消费。

**原因**：属性名写错了，正确的属性名是 `republish-outstanding-events-on-restart`（中间多了 `events`），写错后 Spring Modulith 不会报错，但配置不生效。

**解决方案**：

```properties
# 错误写法（不生效，不会报错）
spring.modulith.events.republish-outstanding-on-restart=true

# 正确写法
spring.modulith.events.republish-outstanding-events-on-restart=true
```

### 5. 事件重放验证步骤

以下为已验证的事件重放完整流程，确认 Spring Modulith 在项目重启后能正确重新消费未完成的事件。

**前提条件**：必须使用持久化数据库（如 H2 文件模式、MySQL、PostgreSQL 等），内存数据库重启后数据丢失无法验证。

**验证步骤**：

1. **切换为持久化数据库**

   将 `application.properties` 中的 H2 连接从内存模式改为文件模式：
   ```properties
   spring.datasource.url=jdbc:h2:file:./data/inventorydb;MODE=MySQL;AUTO_SERVER=TRUE
   ```

2. **模拟事件消费失败**

   在 `InventoryEventListener.handleStockAdded()` 中临时抛出异常：
   ```java
   @ApplicationModuleListener
   public void handleStockAdded(StockAddedEvent event) {
       throw new RuntimeException("模拟消费失败");
   }
   ```

3. **启动应用，创建并完成采购单**

   - 调用 `POST /api/purchase-orders` 创建采购单
   - 调用 `POST /api/purchase-orders/{id}/complete` 完成采购单
   - 由于监听器抛异常，事件消费失败，留在 `EVENT_PUBLICATION` 表中

4. **验证未消费事件**

   ```bash
   curl http://localhost:8080/api/events/publication/count
   # 返回 {"count": 1}  ← 有1个未消费事件

   curl http://localhost:8080/api/events/archive/count
   # 返回 {"count": 0}  ← 归档为0
   ```

5. **恢复监听器代码，重启应用**

   移除临时异常，恢复正常逻辑后重启。

6. **验证事件已被重放消费**

   ```bash
   curl http://localhost:8080/api/events/publication/count
   # 返回 {"count": 0}  ← 未消费事件已清零

   curl http://localhost:8080/api/events/archive/count
   # 返回 {"count": 1}  ← 事件已被重新消费并归档
   ```

**事件重放机制原理**：

```
发布事件 → 写入 EVENT_PUBLICATION 表（COMPLETION_DATE = null）
         → @ApplicationModuleListener 消费
            → 成功：设置 COMPLETION_DATE → 归档到 EVENT_PUBLICATION_ARCHIVE
            → 失败/未消费：COMPLETION_DATE 保持 null → 留在表中
         → 重启时：
            → 扫描 EVENT_PUBLICATION 中 COMPLETION_DATE = null 的记录
            → 反序列化事件 → 重新发布 → 监听器再次消费
```

**保证事件重放的三个必要条件**：

| 条件 | 配置 | 作用 |
|------|------|------|
| 事件持久化 | `spring-modulith-starter-jdbc` + `spring.modulith.events.jdbc.schema-initialization.enabled=true` | 事件写入数据库，不丢失 |
| 重启重放 | `spring.modulith.events.republish-outstanding-events-on-restart=true` | 启动时扫描未完成事件并重新发布 |
| 数据库持久化 | 使用持久化数据库（非内存数据库） | 重启后数据仍然存在 |

---

## 参考项目

本项目参考了 [business-erp](https://gitee.com/lijiancn01/business-erp.git) 的配置方式，特别感谢该项目提供的 Spring Modulith 最佳实践。

## License

MIT License
