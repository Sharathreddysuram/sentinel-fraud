package com.sharath.sentinel.ingestion.api;

import com.sharath.sentinel.ingestion.api.dto.PublishTransactionRequest;
import com.sharath.sentinel.ingestion.events.TransactionEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Edge API of the fraud platform.
 *
 * <p>Accepts a transaction event, validates it, and publishes it to the
 * Kafka topic {@code transactions} keyed by account id. Returns
 * {@code 202 Accepted} immediately — scoring happens asynchronously in the
 * detection-service, so ingestion stays fast under burst load.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Ingestion", description = "Publish transaction events into the fraud pipeline")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private static final String TOPIC = "transactions";

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionController(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    @Operation(summary = "Publish one transaction event",
            description = "Validates the payload and publishes it to the 'transactions' Kafka topic. "
                    + "Responds 202 Accepted — scoring is asynchronous.")
    public ResponseEntity<Map<String, String>> publish(
            @Valid @RequestBody PublishTransactionRequest request) {

        TransactionEvent event = request.toEvent();

        kafkaTemplate.send(TOPIC, event.getAccountId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction {} to topic {}",
                                event.getTransactionId(), TOPIC, ex);
                    } else {
                        log.debug("Published transaction {} to topic {} partition {}",
                                event.getTransactionId(), TOPIC,
                                result.getRecordMetadata().partition());
                    }
                });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "transactionId", event.getTransactionId(),
                        "status", "ACCEPTED"));
    }
}
