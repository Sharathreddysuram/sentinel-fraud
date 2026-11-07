package com.sharath.sentinel.detection.producer;

import com.sharath.sentinel.detection.alert.FraudAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes fraud alerts to the {@code fraud-alerts} Kafka topic.
 * The transaction id is used as the record key so all alerts for one
 * transaction land on the same partition, preserving order.
 */
@Component
public class AlertProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertProducer.class);
    private static final String TOPIC = "fraud-alerts";

    private final KafkaTemplate<String, FraudAlertMessage> kafkaTemplate;

    public AlertProducer(KafkaTemplate<String, FraudAlertMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(FraudAlertMessage message) {
        kafkaTemplate.send(TOPIC, message.getTransactionId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud alert {} to topic {}",
                                message.getAlertId(), TOPIC, ex);
                    } else {
                        log.info("Published fraud alert {} to topic {} partition {}",
                                message.getAlertId(), TOPIC,
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
