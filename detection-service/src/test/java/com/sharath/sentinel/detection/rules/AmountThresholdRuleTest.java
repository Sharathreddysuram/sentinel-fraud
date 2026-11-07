package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Amount rule: fires on >= $10,000 absolute, or >= 3x the account's
 * recent average (needs 3+ historical events). Pure unit test.
 */
class AmountThresholdRuleTest {

    private AmountThresholdRule rule;

    @BeforeEach
    void setUp() {
        RuleProperties props = new RuleProperties();
        props.getAmount().setAbsoluteThreshold(new BigDecimal("10000"));
        props.getAmount().setMultiplierVsAverage(3.0);
        props.getAmount().setWeight(35);
        rule = new AmountThresholdRule(props);
    }

    private TransactionEvent tx(String amount) {
        return new TransactionEvent("tx-1", "acc-1", new BigDecimal(amount),
                "USD", "Test Store", "US", "ONLINE", Instant.now());
    }

    private AccountHistory emptyHistory() {
        return new AccountHistory(List.of());
    }

    @Test
    void twentyThousandTriggersAbsoluteThreshold() {
        RuleResult result = rule.evaluate(tx("20000"), emptyHistory());

        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleName()).isEqualTo("AMOUNT");
        assertThat(result.weight()).isEqualTo(35);
    }

    @Test
    void smallAmountWithNoHistoryDoesNotTrigger() {
        RuleResult result = rule.evaluate(tx("120.50"), emptyHistory());

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void fourTimesAverageTriggersMultiplier() {
        // 3 prior $100 transactions -> average $100, limit 3x = $300.
        TransactionEvent current = tx("400");
        AccountHistory history = new AccountHistory(List.of(
                tx("100"), tx("100"), tx("100")));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleName()).isEqualTo("AMOUNT");
    }

    @Test
    void slightlyAboveAverageDoesNotTrigger() {
        // Average $100, limit $300 — $250 stays under.
        TransactionEvent current = tx("250");
        AccountHistory history = new AccountHistory(List.of(
                tx("100"), tx("100"), tx("100")));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void multiplierNeedsAtLeastThreeHistoricalEvents() {
        // Only 2 prior events -> multiplier check is skipped.
        TransactionEvent current = tx("500");
        AccountHistory history = new AccountHistory(List.of(tx("100"), tx("100")));

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }
}
