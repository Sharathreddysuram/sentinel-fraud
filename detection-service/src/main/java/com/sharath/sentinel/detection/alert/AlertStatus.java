package com.sharath.sentinel.detection.alert;

/** Lifecycle of a fraud alert. */
public enum AlertStatus {
    /** Raised by the rules engine, not yet looked at by an analyst. */
    OPEN,
    /** An analyst has reviewed the alert. */
    REVIEWED
}
