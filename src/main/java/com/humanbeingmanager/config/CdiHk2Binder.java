package com.humanbeingmanager.config;

import com.humanbeingmanager.service.HumanBeingService;
import com.humanbeingmanager.dao.HumanBeingDao;
import jakarta.enterprise.inject.spi.CDI;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class CdiHk2Binder extends AbstractBinder {

    @Override
    protected void configure() {
        bindFactory(new CdiBeanFactory<>(HumanBeingService.class))
            .to(HumanBeingService.class);
            
        bindFactory(new CdiBeanFactory<>(HumanBeingDao.class))
            .to(HumanBeingDao.class);
    }

    private static class CdiBeanFactory<T> implements org.glassfish.hk2.api.Factory<T> {
        private final Class<T> beanClass;
        
        public CdiBeanFactory(Class<T> beanClass) {
            this.beanClass = beanClass;
        }
        
        @Override
        public T provide() {
            try {
                return CDI.current().select(beanClass).get();
            } catch (Exception e) {
                System.err.println("Failed to get CDI bean: " + beanClass.getName() + " - " + e.getMessage());
                return null;
            }
        }
        
        @Override
        public void dispose(T instance) {
        }
    }
}