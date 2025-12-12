package com.humanbeingmanager.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Конфигурация Druid Connection Pool
 * 
 * Когда приложение деплоится на WildFly, пул уже настроен в standalone.xml.
 * Этот бин просто делает lookup готового DataSource в JNDI, без ручной
 * инициализации пула и без требований к переменным окружения.
 */
@Singleton
@Startup
public class DruidDataSourceConfig {
    
    private static final Logger LOGGER = Logger.getLogger(DruidDataSourceConfig.class.getName());
    
    private static final String JNDI_NAME = "java:/PostgresDruidDS";
    
    private DataSource dataSource;
    
    @PostConstruct
    public void init() {
        try {
            LOGGER.info("========== Lookup Druid DataSource from WildFly JNDI ==========");
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(JNDI_NAME);
            LOGGER.info("Found datasource in JNDI: " + JNDI_NAME);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Druid Connection Pool", e);
            throw new RuntimeException("Failed to initialize Druid Connection Pool", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            // Ничего закрывать не нужно — пул управляется контейнером
            dataSource = null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing Druid Connection Pool", e);
        }
    }
    
    /**
     * Получить Druid DataSource (если инициализирован)
     */
    public DataSource getDruidDataSource() {
        return dataSource;
    }
}

