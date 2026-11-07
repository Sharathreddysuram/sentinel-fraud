package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * GEO — impossible-travel check: if the account's previous transaction was
 * in a different country within {@code window-minutes}, someone may be
 * using a stolen card across borders.
 */
@Component
public class GeoAnomalyRule implements FraudRule {

    private final RuleProperties properties;

    public GeoAnomalyRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(TransactionEvent event, AccountHistory history) {
        Duration window = Duration.ofMinutes(properties.getGeo().getWindowMinutes());
        Optional<TransactionEvent> last = history.lastEvent();

        if (last.isEmpty()
                || last.get().getMerchantCountry() == null
                || event.getMerchantCountry() == null
                || last.get().getTimestamp() == null
                || event.getTimestamp() == null) {
            return RuleResult.notTriggered("GEO");
        }

        boolean recentEnough = !last.get().getTimestamp()
                .isBefore(event.getTimestamp().minus(window));
        boolean countryChanged = !last.get().getMerchantCountry()
                .equalsIgnoreCase(event.getMerchantCountry());

        if (recentEnough && countryChanged) {
            return new RuleResult(true, "GEO",
                    "Country changed from " + last.get().getMerchantCountry()
                            + " to " + event.getMerchantCountry()
                            + " within " + window.toMinutes() + " minutes",
                    properties.getGeo().getWeight());
        }
        return RuleResult.notTriggered("GEO");
    }
}
