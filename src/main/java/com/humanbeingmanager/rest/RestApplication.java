package com.humanbeingmanager.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(HumanBeingResource.class);
        classes.add(SpecialOperationsResource.class);
        classes.add(ImportResource.class);
        classes.add(CorsFilter.class);
        return classes;
    }
}