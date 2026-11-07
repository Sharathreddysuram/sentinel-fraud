package com.sharath.sentinel.detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the detection-service — the brain of the fraud platform.
 *
 * <p>Consumes transaction events from the Kafka topic {@code transactions},
 * scores each one with a configurable rules engine (velocity, amount, geo),
 * persists a fraud alert in its own database when the risk score crosses the
 * threshold, and publishes the alert to the {@code fraud-alerts} topic.
 *
 * <p>It also exposes a small synchronous REST surface ({@code /api/simulate},
 * {@code /api/rules}) so the scoring path can be demoed and tuned without
 * any Kafka infrastructure running.
 */
@SpringBootApplication
public class DetectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectionServiceApplication.class, args);
    }
}
