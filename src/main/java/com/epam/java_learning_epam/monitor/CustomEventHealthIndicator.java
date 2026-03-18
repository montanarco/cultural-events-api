package com.epam.java_learning_epam.monitor;

import com.epam.java_learning_epam.service.CulturalEventService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomEventHealthIndicator implements HealthIndicator {

    private final CulturalEventService culturalEventService;

    public CustomEventHealthIndicator(CulturalEventService culturalEventService) {
        this.culturalEventService = culturalEventService;
    }

    @Override
    public Health health() {
        boolean eventsExist = checkForEventsInDatabase();
        if (eventsExist) {
            return Health.up().withDetail("events", "Available").build();
        } else {
            return Health.down().withDetail("events", "None found").build();
        }
    }

    private boolean checkForEventsInDatabase() {
        var events = culturalEventService.findAll();
        return events != null && !events.isEmpty();
    }
}
