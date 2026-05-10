# 库存管理系统 - Spring Boot + Modulith

基于 Spring Boot 3 和 Spring Modulith 2.0.6 构建的库存管理系统演示项目。

## 项目特性

- 商品管理模块
- 库存管理模块
- 采购管理模块（采购单增加库存）
- 销售管理模块（销售单扣减库存）
- 基于 Spring Modulith 的事件驱动架构
- JDBC 持久化事件存储
- 事件归档支持

## 技术栈

- Spring Boot 3.3.0
- Spring Modulith 2.0.6
- Spring Data JPA
- MySQL
- Lombok

## 项目结构

```
src/main/java/com/example/inventorydemo/
├── InventoryDemoApplication.java          # 主应用类
├── EventArchivingConfiguration.java       # 事件归档配置
├── product/                               # 商品模块
│   ├── Product.java
│   ├── ProductRepository.java
│   ├── ProductService.java
│   └── ProductController.java
├── inventory/                             # 库存模块
│   ├── Inventory.java
│   ├── InventoryRepository.java
│   ├── InventoryService.java
│   ├── InventoryController.java
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

确保 MySQL 服务已启动，修改 `application.properties` 中的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_demo
spring.datasource.username=root
spring.datasource.password=root
```

首次运行时，Hibernate 会自动创建所需的表结构。

## 运行项目

1. 确保已安装 JDK 21 和 Maven
2. 启动 MySQL 服务
3. 运行以下命令：

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
- API 端点：
  - 商品：http://localhost:8080/api/products
  - 库存：http://localhost:8080/api/inventory
  - 采购单：http://localhost:8080/api/purchase-orders
  - 销售单：http://localhost:8080/api/sale-orders

## 使用流程

1. **添加商品**：在商品管理页面添加新商品
2. **创建采购单**：在采购管理页面创建采购单，指定商品、数量和单价
3. **完成采购**：点击采购单的"完成"按钮，库存会自动增加
4. **创建销售单**：在销售管理页面创建销售单
5. **完成销售**：点击销售单的"完成"按钮，库存会自动减少（库存不足时会报错）

## 事件机制

项目使用 Spring Modulith 的事件机制：

- 采购单完成时发布 `StockAddedEvent`，库存模块监听并增加库存
- 销售单完成时发布 `StockDeductedEvent`，库存模块监听并减少库存
- 事件通过 JDBC 持久化存储
- 已完成事件会在 5 分钟后自动归档删除

## 注意事项

- 确保 MySQL 服务正常运行
- 确保数据库用户有足够的权限创建表和数据库
- 销售操作会检查库存是否充足，不足时会报错
