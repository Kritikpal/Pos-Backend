package com.kritik.POS.scheduler;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaterializedViewRefresher {

    private final JdbcTemplate entityManager;
    private final Map<String, AtomicBoolean> locks = new ConcurrentHashMap<>();
    // =========================
    // HIGH FREQUENCY (1 min)
    // =========================
    @Scheduled(fixedRate = 60000)
    public void refreshHighFrequency() {
        refresh("mv_hourly_payments");
    }

    // =========================
    // MEDIUM (5 min)
    // =========================
    @Scheduled(fixedRate = 300000)
    public void refreshMediumFrequency() {
        refresh("mv_payment_method_stats");
    }

    // =========================
    // LOW (15 min)
    // =========================
    @Scheduled(fixedRate = 900000)
    public void refreshLowFrequency() {
        refresh("mv_daily_kpi");
        refresh("mv_refund_summary");
        refresh("mv_most_ordered_menu");
    }

    // =========================
    // CORE REFRESH METHOD
    // =========================
    private void refresh(String viewName) {

        AtomicBoolean lock = locks.computeIfAbsent(viewName, k -> new AtomicBoolean(false));

        if (!lock.compareAndSet(false, true)) {
            log.warn("⚠️ Skipping refresh, already running -> {}", viewName);
            return;
        }

        long start = System.currentTimeMillis();

        try {
            log.info("🔄 MV Refresh STARTED -> {}", viewName);

            entityManager.execute(
                    "REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName
            );

            long duration = System.currentTimeMillis() - start;

            log.info("✅ MV Refresh SUCCESS -> {} | duration={} ms", viewName, duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;

            log.error("❌ MV Refresh FAILED -> {} | duration={} ms", viewName, duration, ex);

        } finally {
            lock.set(false);
        }
    }
}