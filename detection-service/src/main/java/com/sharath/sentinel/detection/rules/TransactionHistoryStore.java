package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.events.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-account transaction history.
 *
 * <p>Keeps a bounded deque of recent events per account (24h retention) so
 * the rules engine can look back over short time windows without hitting
 * the database on every Kafka record. For a production deployment this
 * would be backed by Redis or a compacted Kafka state store; the
 * {@link AccountHistory} abstraction keeps that swap painless.
 */
@Component
public class TransactionHistoryStore {

    /** Retention ceiling — anything older is evicted on write. */
    private static final Duration RETENTION = Duration.ofHours(24);

    private final ConcurrentHashMap<String, Deque<TransactionEvent>> store = new ConcurrentHashMap<>();

    /** Record an event for its account, evicting anything older than 24h. */
    public void record(TransactionEvent event) {
        if (event == null || event.getAccountId() == null) {
            return;
        }
        Deque<TransactionEvent> deque =
                store.computeIfAbsent(event.getAccountId(), k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(event);
            evictOlderThan(deque, Instant.now().minus(RETENTION));
        }
    }

    /**
     * Snapshot of the account's events inside {@code window}, oldest first.
     * Used by the rules engine BEFORE the current event is recorded.
     */
    public AccountHistory historyFor(String accountId, Duration window) {
        Deque<TransactionEvent> deque = store.get(accountId);
        if (deque == null) {
            return new AccountHistory(List.of());
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        List<TransactionEvent> inWindow = new ArrayList<>();
        synchronized (deque) {
            for (TransactionEvent e : deque) {
                if (e.getTimestamp() != null && !e.getTimestamp().isBefore(cutoff)) {
                    inWindow.add(e);
                }
            }
        }
        return new AccountHistory(inWindow);
    }

    private void evictOlderThan(Deque<TransactionEvent> deque, Instant cutoff) {
        while (!deque.isEmpty()) {
            TransactionEvent head = deque.peekFirst();
            if (head == null || head.getTimestamp() == null
                    || !head.getTimestamp().isBefore(cutoff)) {
                break;
            }
            deque.pollFirst();
        }
    }
}
