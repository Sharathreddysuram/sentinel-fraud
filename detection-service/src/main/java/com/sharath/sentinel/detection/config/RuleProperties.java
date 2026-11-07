package com.sharath.sentinel.detection.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Fraud rule thresholds, bound from {@code fraud.rules.*} in application.yml.
 *
 * <p>Registered as a mutable {@code @Component} so the thresholds can also be
 * tuned at runtime through {@code PUT /api/rules} — the rules engine reads
 * these values on every evaluation, so changes take effect immediately.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "fraud.rules")
public class RuleProperties {

    private Velocity velocity = new Velocity();
    private Amount amount = new Amount();
    private Geo geo = new Geo();

    @Positive
    private int alertThresholdScore = 50;

    public Velocity getVelocity() {
        return velocity;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    public int getAlertThresholdScore() {
        return alertThresholdScore;
    }

    public void setAlertThresholdScore(int alertThresholdScore) {
        this.alertThresholdScore = alertThresholdScore;
    }

    /** Flags accounts that push too many transactions through a short window. */
    public static class Velocity {
        @Positive
        private int maxTransactions = 5;
        @Positive
        private int windowMinutes = 10;
        private int weight = 40;

        public int getMaxTransactions() {
            return maxTransactions;
        }

        public void setMaxTransactions(int maxTransactions) {
            this.maxTransactions = maxTransactions;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    /** Flags single transactions that are unusually large for the account. */
    public static class Amount {
        private BigDecimal absoluteThreshold = new BigDecimal("10000");
        private double multiplierVsAverage = 3.0;
        private int weight = 35;

        public BigDecimal getAbsoluteThreshold() {
            return absoluteThreshold;
        }

        public void setAbsoluteThreshold(BigDecimal absoluteThreshold) {
            this.absoluteThreshold = absoluteThreshold;
        }

        public double getMultiplierVsAverage() {
            return multiplierVsAverage;
        }

        public void setMultiplierVsAverage(double multiplierVsAverage) {
            this.multiplierVsAverage = multiplierVsAverage;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    /** Flags impossible-travel: the account transacts from a different country too fast. */
    public static class Geo {
        @Positive
        private int windowMinutes = 60;
        private int weight = 25;

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}
