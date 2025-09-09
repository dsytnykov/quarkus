package com.example;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class LifecycleObserver {

    void onStart(@Observes StartupEvent event) {
        System.out.println(">>> StartupEvent observed");
    }

    void onStop(@Observes ShutdownEvent event) {
        System.out.println(">>> ShutdownEvent observed");
    }
}
