package com.example.inventorydemo.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 结算数据源配置（独立数据库）
 *
 * 结算模块使用独立的数据库，只包含 settlements 表（结算业务数据）。
 * 事件持久化由 Spring Modulith 在主库处理，结算模块通过 @ApplicationModuleListener 消费事件。
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.inventorydemo.settlement",
        entityManagerFactoryRef = "settlementEntityManagerFactory",
        transactionManagerRef = "settlementTransactionManager"
)
public class SettlementDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.settlement")
    public DataSourceProperties settlementDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource settlementDataSource() {
        return settlementDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean settlementEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(settlementDataSource())
                .packages("com.example.inventorydemo.settlement")
                .persistenceUnit("settlement")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", "update",
                        "hibernate.dialect", "org.hibernate.dialect.H2Dialect"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager settlementTransactionManager(
            LocalContainerEntityManagerFactoryBean settlementEntityManagerFactory) {
        return new JpaTransactionManager(settlementEntityManagerFactory.getObject());
    }

    @Bean
    public JdbcTemplate settlementJdbcTemplate() {
        return new JdbcTemplate(settlementDataSource());
    }
}
