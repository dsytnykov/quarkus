package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String hello(String name) {
        return "Hello " + name;
    }
}
