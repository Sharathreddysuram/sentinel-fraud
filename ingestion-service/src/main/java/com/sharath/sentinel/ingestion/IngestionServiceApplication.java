package com.sharath.sentinel.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ingestion-service.
 *
 * <p>This is the edge of the fraud platform: it accepts transaction events
 * over REST, validates them, and publishes them to the Kafka topic
 * {@code transactions} (keyed by account id). It owns no database and keeps
 * no state — a thin, horizontally scalable gateway in front of the pipeline.
 */
@SpringBootApplication
public class IngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
