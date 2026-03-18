package com.epam.java_learning_epam.monitor;

import com.epam.java_learning_epam.model.EventType;
import com.epam.java_learning_epam.repository.CulturalEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class EventMetricsService {

    private final Counter eventsCreatedCounter;
    private final Counter eventsDeletedCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Timer findAllTimer;
    private final Map<EventType, Counter> eventsByTypeCounters;

    public EventMetricsService(MeterRegistry registry, CulturalEventRepository repo) {

        this.eventsCreatedCounter = Counter.builder("events_created_total")
                .description("Total cultural events created")
                .register(registry);

        this.eventsDeletedCounter = Counter.builder("events_deleted_total")
                .description("Total cultural events deleted")
                .register(registry);

        this.loginSuccessCounter = Counter.builder("auth_login_attempts_total")
                .description("Total authentication attempts")
                .tag("status", "success")
                .register(registry);

        this.loginFailureCounter = Counter.builder("auth_login_attempts_total")
                .description("Total authentication attempts")
                .tag("status", "failure")
                .register(registry);

        this.findAllTimer = Timer.builder("events_service_duration_seconds")
                .description("Time taken by CulturalEventService.findAll()")
                .tag("operation", "findAll")
                .register(registry);

        // Gauge — repo.count() is called on every Prometheus scrape
        Gauge.builder("events_total_in_db", repo, r -> r.count())
                .description("Current number of cultural events in the database")
                .register(registry);

        // Pre-register one counter per event type so all appear from startup
        this.eventsByTypeCounters = new EnumMap<>(EventType.class);
        for (EventType type : EventType.values()) {
            eventsByTypeCounters.put(type, Counter.builder("events_by_type_total")
                    .description("Total events created per type")
                    .tag("type", type.name())
                    .register(registry));
        }
    }

    public void recordEventCreated(EventType type) {
        eventsCreatedCounter.increment();
        if (type != null) {
            eventsByTypeCounters.get(type).increment();
        }
    }

    public void recordEventDeleted() {
        eventsDeletedCounter.increment();
    }

    public void recordLoginAttempt(boolean success) {
        if (success) {
            loginSuccessCounter.increment();
        } else {
            loginFailureCounter.increment();
        }
    }

    public <T> T timeFindAll(Supplier<T> supplier) {
        return findAllTimer.record(supplier);
    }
}
