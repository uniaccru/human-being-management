package com.humanbeingmanager.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


@ApplicationScoped
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private EntityManagerFactory entityManagerFactory;

    private EntityManagerFactory createEntityManagerFactory() {
        if (entityManagerFactory == null) {
            try {
                LOGGER.info("Initializing database connection...");
                
                Map<String, Object> properties = new HashMap<>();
                
                String databaseUrl = System.getenv("DATABASE_URL");
                String pgUser = System.getenv("PGUSER");
                String pgPassword = System.getenv("PGPASSWORD");
                
                if (databaseUrl == null) {
                    LOGGER.severe("Missing database configuration. Required environment variable: DATABASE_URL");
                    throw new RuntimeException("Database configuration missing");
                }
                
                String jdbcUrl = databaseUrl;
                String username = pgUser;
                String password = pgPassword;
                
                if (databaseUrl.startsWith("postgresql://")) {
                    String withoutScheme = databaseUrl.substring("postgresql://".length());

                    int atIndex = withoutScheme.indexOf('@');
                    if (atIndex > 0) {
                        String userInfo = withoutScheme.substring(0, atIndex);
                        String hostPart = withoutScheme.substring(atIndex + 1);

                        String[] credentials = userInfo.split(":", 2);
                        if (username == null && credentials.length > 0) {
                            username = credentials[0];
                        }
                        if (password == null && credentials.length > 1) {
                            password = credentials[1];
                        }

                        jdbcUrl = "jdbc:postgresql://" + hostPart;
                    } else {
                        jdbcUrl = "jdbc:postgresql://" + withoutScheme;
                    }
                } else if (!databaseUrl.startsWith("jdbc:postgresql://")) {
                    LOGGER.severe("Invalid DATABASE_URL format. Expected postgresql:// or jdbc:postgresql:// but got: " + databaseUrl.substring(0, Math.min(databaseUrl.length(), 20)) + "...");
                    throw new RuntimeException("Invalid DATABASE_URL format");
                }
                
                if (username == null || password == null) {
                    LOGGER.severe("Missing database credentials. Provide PGUSER and PGPASSWORD environment variables or embed credentials in DATABASE_URL");
                    throw new RuntimeException("Database credentials missing");
                }

                properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
                properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
                properties.put("jakarta.persistence.jdbc.user", username);
                properties.put("jakarta.persistence.jdbc.password", password);

                properties.put("eclipselink.ddl-generation", "create-or-extend-tables");
                properties.put("eclipselink.ddl-generation.output-mode", "database");
                properties.put("eclipselink.logging.level", "INFO");
                properties.put("eclipselink.logging.parameters", "true");
                properties.put("eclipselink.target-database", "PostgreSQL");

                properties.put("eclipselink.connection-pool.default.initial", "5");
                properties.put("eclipselink.connection-pool.default.min", "5");
                properties.put("eclipselink.connection-pool.default.max", "20");
                
                entityManagerFactory = Persistence.createEntityManagerFactory("HumanBeingPU", properties);
                
                LOGGER.info("Database connection initialized successfully");

                EntityManager em = entityManagerFactory.createEntityManager();
                try {
                    em.getTransaction().begin();
                    em.getTransaction().commit();
                    LOGGER.info("Database connection test successful");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Database connection test failed", e);
                    throw e;
                } finally {
                    em.close();
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize database connection", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        }
        return entityManagerFactory;
    }

    @Produces
    @ApplicationScoped
    public EntityManager produceEntityManager() {
        return createEntityManagerFactory().createEntityManager();
    }

    public void destroy() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
            LOGGER.info("EntityManagerFactory closed");
        }
    }
}