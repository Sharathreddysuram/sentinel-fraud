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
 * Engine-level scoring: weights combine, threshold decides fraud,
 * and the evaluated event is recorded for the NEXT evaluation.
 */
class RulesEngineTest {

    private RulesEngine engine;
    private TransactionHistoryStore store;

    @BeforeEach
    void setUp() {
        RuleProperties props = new RuleProperties(); // defaults: 40/35/25, threshold 50
        store = new TransactionHistoryStore();
        List<FraudRule> rules = List.of(
                new VelocityRule(props),
                new AmountThresholdRule(props),
                new GeoAnomalyRule(props));
        engine = new RulesEngine(rules, props, store);
    }

    private TransactionEvent tx(String id, String account, String amount, String country) {
        return new TransactionEvent(id, account, new BigDecimal(amount),
                "USD", "Test Store", country, "ONLINE", Instant.now());
    }

    @Test
    void velocityPlusGeoScores65AndIsFraud() {
        String account = "acc-combo";
        // 4 prior US transactions, then a 5th from NG:
        // VELOCITY (5 in window, weight 40) + GEO (US->NG, weight 25) = 65 >= 50.
        for (int i = 1; i <= 4; i++) {
            engine.evaluate(tx("tx-prior-" + i, account, "30.00", "US"));
        }

        RulesEngine.EvaluationResult result =
                engine.evaluate(tx("tx-suspicious", account, "45.00", "NG"));

        assertThat(result.riskScore()).isEqualTo(65);
        assertThat(result.fraud()).isTrue();
        assertThat(result.triggeredRules())
                .extracting(RuleResult::ruleName)
                .containsExactlyInAnyOrder("VELOCITY", "GEO");
    }

    @Test
    void singleSmallTransactionIsClean() {
        RulesEngine.EvaluationResult result =
                engine.evaluate(tx("tx-clean", "acc-clean", "25.00", "US"));

        assertThat(result.riskScore()).isZero();
        assertThat(result.fraud()).isFalse();
        assertThat(result.triggeredRules()).isEmpty();
    }

    @Test
    void amountRuleAloneBelowThresholdIsNotFraud() {
        // $12,000 fires AMOUNT (weight 35) but 35 < 50, so no alert.
        RulesEngine.EvaluationResult result =
                engine.evaluate(tx("tx-big", "acc-big", "12000", "US"));

        assertThat(result.riskScore()).isEqualTo(35);
        assertThat(result.fraud()).isFalse();
    }

    @Test
    void evaluatedEventIsVisibleToNextEvaluation() {
        String account = "acc-ordering";
        // First evaluation records the event; a second identical-amount
        // event right after sees it in history (velocity counts both).
        engine.evaluate(tx("tx-1", account, "10.00", "US"));
        engine.evaluate(tx("tx-2", account, "10.00", "US"));
        engine.evaluate(tx("tx-3", account, "10.00", "US"));
        engine.evaluate(tx("tx-4", account, "10.00", "US"));

        RulesEngine.EvaluationResult fifth =
                engine.evaluate(tx("tx-5", account, "10.00", "US"));

        // 5 events in window -> velocity fires -> score 40, below threshold.
        assertThat(fifth.triggeredRules())
                .extracting(RuleResult::ruleName)
                .contains("VELOCITY");
    }
}
