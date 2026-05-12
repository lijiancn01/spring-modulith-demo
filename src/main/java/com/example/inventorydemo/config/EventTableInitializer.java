package com.example.inventorydemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class EventTableInitializer {

    private final DataSource dataSource;

    @Autowired
    public EventTableInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        try (Connection connection = dataSource.getConnection()) {
            String createPublicationTable = """
                CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION (
                    ID VARCHAR(36) NOT NULL PRIMARY KEY,
                    EVENT_TYPE VARCHAR(512) NOT NULL,
                    LISTENER_ID VARCHAR(512) NOT NULL,
                    PUBLICATION_DATE TIMESTAMP(6) NOT NULL,
                    COMPLETION_DATE TIMESTAMP(6),
                    SERIALIZED_EVENT TEXT NOT NULL
                );
                """;
            try (Statement statement = connection.createStatement()) {
                statement.execute(createPublicationTable);
                log.info("Created EVENT_PUBLICATION table");
            }

            String createArchiveTable = """
                CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION_ARCHIVE (
                    ID VARCHAR(36) NOT NULL PRIMARY KEY,
                    EVENT_TYPE VARCHAR(512) NOT NULL,
                    LISTENER_ID VARCHAR(512) NOT NULL,
                    PUBLICATION_DATE TIMESTAMP(6) NOT NULL,
                    COMPLETION_DATE TIMESTAMP(6),
                    SERIALIZED_EVENT TEXT NOT NULL,
                    ARCHIVED_AT TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
                );
                """;
            try (Statement statement = connection.createStatement()) {
                statement.execute(createArchiveTable);
                log.info("Created EVENT_PUBLICATION_ARCHIVE table");
            }
        } catch (SQLException e) {
            log.error("Failed to create event tables", e);
            throw new RuntimeException("Failed to create event tables", e);
        }
    }
}