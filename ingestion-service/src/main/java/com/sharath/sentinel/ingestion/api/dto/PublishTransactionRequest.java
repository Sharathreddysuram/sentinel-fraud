package com.sharath.sentinel.ingestion.api.dto;

import com.sharath.sentinel.ingestion.events.TransactionEvent;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for {@code POST /api/transactions}.
 * Mirrors {@link TransactionEvent}; the timestamp defaults to now when absent.
 */
public class PublishTransactionRequest {

    @NotBlank
    @Size(max = 64)
    private String transactionId;

    @NotBlank
    @Size(max = 64)
    private String accountId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be positive")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 8)
    private String currency;

    private String merchant;

    @NotBlank
    @Size(min = 2, max = 2, message = "merchantCountry must be a 2-letter ISO code")
    private String merchantCountry;

    private String channel;

    private Instant timestamp;

    public PublishTransactionRequest() {
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

    /** Converts to the domain event; defaults timestamp to now when absent. */
    public TransactionEvent toEvent() {
        return new TransactionEvent(
                transactionId,
                accountId,
                amount,
                currency,
                merchant,
                merchantCountry,
                channel,
                timestamp != null ? timestamp : Instant.now());
    }
}
