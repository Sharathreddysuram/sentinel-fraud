package com.sharath.sentinel.alerts.view;

/** Lifecycle of an alert in the read model. */
public enum AlertViewStatus {
    /** Raised by detection, not yet looked at by an analyst. */
    OPEN,
    /** An analyst has reviewed the alert. */
    REVIEWED
}
