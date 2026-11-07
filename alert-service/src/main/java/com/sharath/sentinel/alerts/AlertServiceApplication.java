package com.sharath.sentinel.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the alert-service — the analyst-facing read side of the
 * fraud platform.
 *
 * <p>It consumes fraud alerts from the Kafka topic {@code fraud-alerts} and
 * builds its own read-model database (CQRS-lite: each service owns its
 * data). Analysts list, fetch, and review alerts through its REST API.
 * It never talks to the detection-service directly — Kafka is the only
 * coupling between the two.
 */
@SpringBootApplication
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
