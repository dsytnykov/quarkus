package com.example;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Inject
    Greeter greeter;

    @Override
    public int run(String... args) {
        System.out.println(greeter.hello("from Quarkus + CDI"));
        // Keep the app alive so you can inspect logs/init; Ctrl+C to exit.
        Quarkus.waitForExit();
        return 0;
    }
}
