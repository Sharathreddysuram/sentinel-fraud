package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Velocity rule: 5+ transactions within 10 minutes fires (weight 40).
 * Pure unit test — no Kafka, no Spring context.
 */
class VelocityRuleTest {

    private VelocityRule rule;
    private TransactionHistoryStore store;

    @BeforeEach
    void setUp() {
        RuleProperties props = new RuleProperties();
        props.getVelocity().setMaxTransactions(5);
        props.getVelocity().setWindowMinutes(10);
        props.getVelocity().setWeight(40);
        rule = new VelocityRule(props);
        store = new TransactionHistoryStore();
    }

    private TransactionEvent tx(String id, String account) {
        return new TransactionEvent(id, account, new BigDecimal("25.00"),
                "USD", "Test Store", "US", "POS", Instant.now());
    }

    @Test
    void fiveRapidTransactionsTrigger() {
        String account = "acc-velocity-1";
        // 4 prior transactions recorded, the 5th is the one being evaluated.
        for (int i = 1; i <= 4; i++) {
            store.record(tx("tx-prior-" + i, account));
        }
        TransactionEvent current = tx("tx-current", account);
        AccountHistory history = store.historyFor(account, Duration.ofHours(24));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleName()).isEqualTo("VELOCITY");
        assertThat(result.weight()).isEqualTo(40);
    }

    @Test
    void twoTransactionsDoNotTrigger() {
        String account = "acc-velocity-2";
        store.record(tx("tx-prior-1", account));
        TransactionEvent current = tx("tx-current", account);
        AccountHistory history = store.historyFor(account, Duration.ofHours(24));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void oldTransactionsOutsideWindowDoNotCount() {
        String account = "acc-velocity-3";
        // 10 events, but all 2 hours old — outside the 10-minute window.
        for (int i = 1; i <= 10; i++) {
            TransactionEvent old = tx("tx-old-" + i, account);
            old.setTimestamp(Instant.now().minus(Duration.ofHours(2)));
            store.record(old);
        }
        TransactionEvent current = tx("tx-current", account);
        AccountHistory history = store.historyFor(account, Duration.ofHours(24));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }
}
