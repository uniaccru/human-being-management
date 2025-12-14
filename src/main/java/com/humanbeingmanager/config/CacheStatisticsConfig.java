package com.humanbeingmanager.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
public class CacheStatisticsConfig {
    
    private static final Logger LOGGER = Logger.getLogger(CacheStatisticsConfig.class.getName());
    
    private static final String CACHE_STATISTICS_ENABLED_PROP = "cache.statistics.enabled";
    private static final String DEFAULT_ENABLED = "false";
    
    private Boolean enabled = null;
    
    public boolean isStatisticsEnabled() {
        if (enabled == null) {
            String propValue = System.getProperty(CACHE_STATISTICS_ENABLED_PROP, DEFAULT_ENABLED);
            enabled = Boolean.parseBoolean(propValue);
            LOGGER.info("Cache statistics logging enabled: " + enabled);
        }
        return enabled;
    }
    
    //вызываем этот метод чтоб включить логирование
    public void setStatisticsEnabled(boolean enabled) {
        this.enabled = enabled;
        System.setProperty(CACHE_STATISTICS_ENABLED_PROP, String.valueOf(enabled));
        LOGGER.info("Cache statistics logging " + (enabled ? "enabled" : "disabled"));
    }
}






