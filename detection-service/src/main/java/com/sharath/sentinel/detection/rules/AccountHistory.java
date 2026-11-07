package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.events.TransactionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Read-only view of one account's recent transactions.
 * Events are ordered oldest-first. The event currently being evaluated
 * is deliberately NOT part of this history (evaluate-before-record).
 */
public class AccountHistory {

    private final List<TransactionEvent> recentEvents;

    public AccountHistory(List<TransactionEvent> recentEvents) {
        this.recentEvents = List.copyOf(recentEvents);
    }

    /** All known recent events for the account, oldest first. */
    public List<TransactionEvent> recentEvents() {
        return recentEvents;
    }

    /** Events inside the trailing window, relative to {@code now}. */
    public List<TransactionEvent> eventsInWindow(Duration window, Instant now) {
        Instant cutoff = now.minus(window);
        return recentEvents.stream()
                .filter(e -> e.getTimestamp() != null && !e.getTimestamp().isBefore(cutoff))
                .toList();
    }

    /** The most recent transaction on record, if any. */
    public Optional<TransactionEvent> lastEvent() {
        if (recentEvents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recentEvents.get(recentEvents.size() - 1));
    }

    public boolean isEmpty() {
        return recentEvents.isEmpty();
    }
}
