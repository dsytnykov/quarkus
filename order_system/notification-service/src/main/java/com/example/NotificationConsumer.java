package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class NotificationConsumer {

    @Incoming("user-events-in")
    public void onUserEvent(String payload) {
        System.out.println("[notification-service] user event => " + payload);
    }

    @Incoming("order-events-in")
    public void onOrderEvent(String payload) {
        System.out.println("[notification-service] order event => " + payload);
    }
}
