package com.algorythm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AlgoRythm backend.
 *
 * <p>Boots the Spring application context and starts the embedded web server.
 * Feature code lives under the sibling packages (controller, service, repository,
 * config, model).
 */
@SpringBootApplication
public class AlgorythmApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgorythmApplication.class, args);
    }
}
