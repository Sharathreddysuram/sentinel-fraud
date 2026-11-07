package com.sharath.sentinel.ingestion.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single money-movement event, published to the Kafka topic
 * {@code transactions} by the ingestion-service.
 *
 * <p>The schema is intentionally duplicated (rather than shared via a common
 * jar) so each service owns its contract and can evolve without forcing a
 * lock-step deployment of the others. In production this schema would be
 * governed by a registry (e.g. Confluent Schema Registry / Avro).
 *
 * <p>Explicit no-arg constructor + getters/setters are kept so Jackson
 * serialization is obvious and never surprises anyone.
 */
public class TransactionEvent {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String merchant;
    private String merchantCountry; // ISO 3166-1 alpha-2, e.g. "US", "NG"
    private String channel;         // e.g. ONLINE, POS, ATM
    private Instant timestamp;

    /** Required by Jackson. */
    public TransactionEvent() {
    }

    public TransactionEvent(String transactionId, String accountId, BigDecimal amount,
                            String currency, String merchant, String merchantCountry,
                            String channel, Instant timestamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.merchant = merchant;
        this.merchantCountry = merchantCountry;
        this.channel = channel;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getMerchantCountry() {
        return merchantCountry;
    }

    public void setMerchantCountry(String merchantCountry) {
        this.merchantCountry = merchantCountry;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TransactionEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", merchant='" + merchant + '\'' +
                ", merchantCountry='" + merchantCountry + '\'' +
                ", channel='" + channel + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
