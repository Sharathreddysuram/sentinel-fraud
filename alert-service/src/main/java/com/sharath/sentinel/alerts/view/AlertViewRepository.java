package com.sharath.sentinel.alerts.view;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertViewRepository extends JpaRepository<AlertView, UUID> {

    /** Newest alerts first — the default analyst queue ordering. */
    Page<AlertView> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
