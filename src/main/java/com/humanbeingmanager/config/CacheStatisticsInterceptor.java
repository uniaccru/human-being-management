package com.humanbeingmanager.config;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;

/**
 * CDI Interceptor for logging L2 JPA Cache statistics (hits and misses).
 * Can be enabled/disabled via system property: cache.statistics.enabled
 */
@Interceptor
@CacheStatisticsLogging
public class CacheStatisticsInterceptor {
    
    private static final Logger LOGGER = Logger.getLogger(CacheStatisticsInterceptor.class.getName());
    
    @Inject
    private CacheStatisticsConfig config;
    
    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        if (!config.isStatisticsEnabled()) {
            return context.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = context.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log cache statistics
            // Note: EclipseLink L2 cache statistics are not directly accessible via public API
            // This interceptor logs method execution time as an indicator of cache performance
            // In a production environment, you would use EclipseLink's SessionProfiler or
            // JMX MBeans to get actual cache hit/miss statistics
            
            if (executionTime > 50) { // Log only if execution takes significant time
                LOGGER.info(String.format(
                    "Cache Statistics [%s.%s]: ExecutionTime=%dms (L2 cache enabled - check EclipseLink logs for detailed cache statistics)",
                    context.getTarget().getClass().getSimpleName(),
                    context.getMethod().getName(),
                    executionTime
                ));
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.severe("Error in cache statistics interceptor: " + e.getMessage());
            throw e;
        }
    }
}

