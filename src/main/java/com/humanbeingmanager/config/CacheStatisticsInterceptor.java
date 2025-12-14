package com.humanbeingmanager.config;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;


@Interceptor 
@CacheStatisticsLogging //будет срабатывать везде где естьб эта аннтоация
public class CacheStatisticsInterceptor {
    
    private static final Logger LOGGER = Logger.getLogger(CacheStatisticsInterceptor.class.getName());
    
    //проверяет свойство включено ли логирование
    private static boolean isStatisticsEnabled() {
        String propValue = System.getProperty("cache.statistics.enabled", "false");
        return Boolean.parseBoolean(propValue);
    }
    
    //будет вызван до и после каждого метода
    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        boolean enabled = isStatisticsEnabled();
        LOGGER.info(String.format(
            "CacheStatisticsInterceptor invoked for [%s.%s], statistics enabled: %s",
            context.getTarget().getClass().getSimpleName(),
            context.getMethod().getName(),
            enabled
        ));
        
        if (!enabled) {
            return context.proceed();
        } //если выключено то пропускаем
        
        long startTime = System.currentTimeMillis();
        
        try {
            LOGGER.info(String.format(
                "Cache Statistics [%s.%s]: Method called (L2 cache statistics enabled)",
                context.getTarget().getClass().getSimpleName(),
                context.getMethod().getName()
            ));
            
            Object result = context.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            LOGGER.info(String.format(
                "Cache Statistics [%s.%s]: ExecutionTime=%dms (L2 cache enabled - check EclipseLink logs for detailed cache statistics)",
                context.getTarget().getClass().getSimpleName(),
                context.getMethod().getName(),
                executionTime
            ));
            
            return result;
        } catch (Exception e) {
            LOGGER.severe("Error in cache statistics interceptor: " + e.getMessage());
            throw e;
        }
    }
}

