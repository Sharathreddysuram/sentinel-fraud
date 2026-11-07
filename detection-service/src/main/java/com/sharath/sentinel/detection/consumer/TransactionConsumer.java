package com.sharath.sentinel.detection.consumer;

import com.sharath.sentinel.detection.alert.AlertService;
import com.sharath.sentinel.detection.alert.FraudAlert;
import com.sharath.sentinel.detection.alert.FraudAlertMessage;
import com.sharath.sentinel.detection.events.TransactionEvent;
import com.sharath.sentinel.detection.producer.AlertProducer;
import com.sharath.sentinel.detection.rules.RuleResult;
import com.sharath.sentinel.detection.rules.RulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes transaction events from the {@code transactions} topic.
 *
 * <p>For each event: score it with the rules engine; when the risk score
 * crosses the threshold, persist a fraud alert and publish it to the
 * {@code fraud-alerts} topic. The whole body is guarded so a single
 * poisoned record can never take the listener down.
 */
@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private final RulesEngine rulesEngine;
    private final AlertService alertService;
    private final AlertProducer alertProducer;

    public TransactionConsumer(RulesEngine rulesEngine,
                               AlertService alertService,
                               AlertProducer alertProducer) {
        this.rulesEngine = rulesEngine;
        this.alertService = alertService;
        this.alertProducer = alertProducer;
    }

    @KafkaListener(topics = "transactions", groupId = "sentinel-detection")
    public void onTransaction(TransactionEvent event) {
        try {
            if (event == null || event.getTransactionId() == null) {
                log.warn("Skipping malformed transaction event: {}", event);
                return;
            }

            RulesEngine.EvaluationResult result = rulesEngine.evaluate(event);

            if (result.fraud()) {
                FraudAlert alert = alertService.saveAlert(event, result);
                List<String> ruleNames = result.triggeredRules().stream()
                        .map(RuleResult::ruleName)
                        .toList();
                alertProducer.send(new FraudAlertMessage(
                        alert.getId(),
                        event.getTransactionId(),
                        event.getAccountId(),
                        event.getAmount(),
                        result.riskScore(),
                        ruleNames,
                        alert.getCreatedAt()));
                log.info("FRAUD detected: tx={} account={} score={} rules={}",
                        event.getTransactionId(), event.getAccountId(),
                        result.riskScore(), ruleNames);
            } else {
                log.debug("Clean transaction: tx={} score={}",
                        event.getTransactionId(), result.riskScore());
            }
        } catch (Exception ex) {
            // Never let one bad record kill the consumer loop.
            log.error("Error processing transaction event, skipping: {}", event, ex);
        }
    }
}
