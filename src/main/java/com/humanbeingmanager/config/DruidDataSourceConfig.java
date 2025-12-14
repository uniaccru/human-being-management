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

@Singleton
@Startup
public class DruidDataSourceConfig {
    
    private static final Logger LOGGER = Logger.getLogger(DruidDataSourceConfig.class.getName());
    
    private static final String JNDI_NAME = "java:/PostgresDruidDS";
    
    private DataSource dataSource; //здесь храним ссылку на пул соединений
    
    @PostConstruct
    public void init() {
        try {
            LOGGER.info("Lookup Druid DataSource");
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(JNDI_NAME); //ищем готовый датасурс по имени 
            //найди в контексте JNDI по имени -> преобразуй его к датасурс -> сохрани ссылку в переменную
            LOGGER.info("Found datasource in JNDI");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Druid Connection Pool", e);
            throw new RuntimeException("Failed to initialize Druid Connection Pool", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            dataSource = null; //обнуляем ссылку на датасурс. не закрываем пул, вайлдфлай сделает это сам
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing Druid Connection Pool", e);
        }
    }
    
    public DataSource getDruidDataSource() {
        return dataSource; //метод для получение датасурс другими частями приложения
    }
}

