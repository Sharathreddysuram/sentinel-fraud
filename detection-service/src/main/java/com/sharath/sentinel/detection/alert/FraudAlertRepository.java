package com.sharath.sentinel.detection.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
}
