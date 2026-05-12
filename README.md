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

## 关键配置说明

```properties
# 启用事件持久化表结构自动初始化
spring.modulith.events.jdbc.schema-initialization.enabled=true

# 事件完成模式为 archive（归档）
spring.modulith.events.completion-mode=archive

# 重启时重新发布未完成的事件
spring.modulith.events.republish-outstanding-on-restart=true
```

## 注意事项

- 默认使用 H2 内存数据库，重启后数据会丢失
- 如需持久化数据，配置使用 MySQL
- 销售操作会检查库存是否充足，不足时会报错

---

## 常见问题与解决方案（踩坑记录）

### 1. Maven 依赖下载超时

**问题**：使用阿里云镜像时连接超时，无法下载依赖。

**解决方案**：
- 使用 Maven Central 官方仓库
- 配置 Maven 代理（如公司内网代理）
- 修改 `~/.m2/settings.xml` 配置：
```xml
<settings>
    <proxies>
        <proxy>
            <id>example-proxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>proxy.example.com</host>
            <port>8080</port>
            <username>proxyuser</username>
            <password>somepassword</password>
            <nonProxyHosts>www.google.com|*.example.com</nonProxyHosts>
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

---

## 参考项目

本项目参考了 [business-erp](https://gitee.com/lijiancn01/business-erp.git) 的配置方式，特别感谢该项目提供的 Spring Modulith 最佳实践。

## License

MIT License