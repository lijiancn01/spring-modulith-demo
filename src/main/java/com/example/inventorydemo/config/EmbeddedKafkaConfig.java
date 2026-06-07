package com.example.inventorydemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

/**
 * 嵌入式 Kafka 配置（仅 Kafka 模式生效）
 *
 * 开发环境使用嵌入式 Kafka，无需安装独立的 Kafka 服务。
 * 生产环境替换为真实 Kafka 连接即可。
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "kafka")
public class EmbeddedKafkaConfig {

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaZKBroker(1)
                .kafkaPorts(9092);
    }
}
