package com.university.smartcampus;

import com.university.smartcampus.service.InMemoryStorage;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        packages("com.university.smartcampus");
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(InMemoryStorage.class).in(Singleton.class);
            }
        });
    }
}
