package com.humanbeingmanager.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Выполняет миграции базы данных при старте приложения
 */
@Singleton
@Startup
public class DatabaseMigration {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());
    
    @Resource(lookup = "java:/PostgresDS")
    private DataSource dataSource;
    
    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void init() {
        try {
            LOGGER.info("Starting database migration...");
            
            // Добавление колонки file_key в таблицу import_history, если её нет
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute("ALTER TABLE import_history ADD COLUMN IF NOT EXISTS file_key VARCHAR(255)");
                LOGGER.info("Migration completed: file_key column added to import_history table");
                
            } catch (Exception e) {
                // Игнорируем ошибку, если колонка уже существует или таблица не существует
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("already exists")) {
                    LOGGER.info("Column file_key already exists in import_history table - skipping migration");
                } else {
                    LOGGER.log(Level.WARNING, "Migration for file_key column: " + errorMsg, e);
                }
            }
            
            LOGGER.info("Database migration finished");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during database migration", e);
            // Не бросаем исключение, чтобы приложение могло запуститься
        }
    }
}

