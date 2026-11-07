package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geo rule: fires when the country changes within 60 minutes
 * (impossible-travel signal). Pure unit test.
 */
class GeoAnomalyRuleTest {

    private GeoAnomalyRule rule;

    @BeforeEach
    void setUp() {
        RuleProperties props = new RuleProperties();
        props.getGeo().setWindowMinutes(60);
        props.getGeo().setWeight(25);
        rule = new GeoAnomalyRule(props);
    }

    private TransactionEvent tx(String country, Instant timestamp) {
        return new TransactionEvent("tx-1", "acc-1", new BigDecimal("50.00"),
                "USD", "Test Store", country, "POS", timestamp);
    }

    @Test
    void usThenNigeriaWithinSixtyMinutesTriggers() {
        Instant now = Instant.now();
        AccountHistory history = new AccountHistory(List.of(tx("US", now.minus(Duration.ofMinutes(20)))));
        TransactionEvent current = tx("NG", now);

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleName()).isEqualTo("GEO");
        assertThat(result.weight()).isEqualTo(25);
    }

    @Test
    void sameCountryDoesNotTrigger() {
        Instant now = Instant.now();
        AccountHistory history = new AccountHistory(List.of(tx("US", now.minus(Duration.ofMinutes(20)))));
        TransactionEvent current = tx("US", now);

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void countryChangeAfterWindowDoesNotTrigger() {
        Instant now = Instant.now();
        // Previous transaction 3 hours ago — outside the 60-minute window.
        AccountHistory history = new AccountHistory(List.of(tx("US", now.minus(Duration.ofHours(3)))));
        TransactionEvent current = tx("NG", now);

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void firstEverTransactionDoesNotTrigger() {
        RuleResult result = rule.evaluate(tx("NG", Instant.now()),
                new AccountHistory(List.of()));

        assertThat(result.triggered()).isFalse();
    }

    @Test
    void countryComparisonIsCaseInsensitive() {
        Instant now = Instant.now();
        AccountHistory history = new AccountHistory(List.of(tx("us", now.minus(Duration.ofMinutes(5)))));
        TransactionEvent current = tx("US", now);

        RuleResult result = rule.evaluate(current, history);

        assertThat(result.triggered()).isFalse();
    }
}
