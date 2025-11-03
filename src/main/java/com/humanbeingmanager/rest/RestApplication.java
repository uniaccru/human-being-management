package com.humanbeingmanager.rest;

import com.humanbeingmanager.config.CdiHk2Binder;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    
    public RestApplication() {
        register(HumanBeingResource.class);
        register(SpecialOperationsResource.class);
        register(ImportResource.class);
        register(CorsFilter.class);
        register(new CdiHk2Binder());
    }
}