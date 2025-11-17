package com.humanbeingmanager.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Менеджер блокировок для координат на уровне приложения.
 * Обеспечивает синхронизацию при создании объектов с одинаковыми координатами.
 */
@jakarta.enterprise.context.ApplicationScoped
public class CoordinateLockManager {
    
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    /**
     * Получает блокировку для конкретных координат.
     * Если блокировка не существует, создает новую.
     */
    public ReentrantLock getLock(Integer x, double y) {
        String key = x + "," + y;
        return locks.computeIfAbsent(key, k -> new ReentrantLock(true)); // fair lock
    }
    
    /**
     * Очищает неиспользуемые блокировки (опционально, для управления памятью).
     * Можно вызывать периодически через scheduler.
     */
    public void cleanupUnusedLocks() {
        locks.entrySet().removeIf(entry -> !entry.getValue().isLocked() && entry.getValue().getQueueLength() == 0);
    }
}

