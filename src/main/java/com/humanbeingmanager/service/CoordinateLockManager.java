package com.humanbeingmanager.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@jakarta.enterprise.context.ApplicationScoped
public class CoordinateLockManager {
    
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    public ReentrantLock getLock(Integer x, double y) {
        String key = x + "," + y;
        return locks.computeIfAbsent(key, k -> new ReentrantLock(true)); 
    }
}

