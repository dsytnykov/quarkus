package com.example;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class Greeter {
    @ConfigProperty(name = "greeting.prefix", defaultValue = "Hello")
    String prefix;

    String hello(String who) { return prefix + " " + who; }
}
