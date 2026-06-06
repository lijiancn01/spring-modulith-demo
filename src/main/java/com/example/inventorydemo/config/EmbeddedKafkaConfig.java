package com.example.inventorydemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

/**
 * 嵌入式 Kafka 配置
 *
 * 开发环境使用嵌入式 Kafka，无需安装独立的 Kafka 服务。
 * 生产环境替换为真实 Kafka 连接即可。
 */
@Configuration
public class EmbeddedKafkaConfig {

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaKraftBroker(1, 1,
                "purchase-order-completed", "sale-order-completed");
    }
}
