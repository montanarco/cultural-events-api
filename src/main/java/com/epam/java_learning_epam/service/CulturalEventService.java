package com.epam.java_learning_epam.service;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.monitor.EventMetricsService;
import com.epam.java_learning_epam.repository.CulturalEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CulturalEventService {

    private final CulturalEventRepository culturalEventRepo;
    private final EventMetricsService metricsService;

    public CulturalEventService(CulturalEventRepository repository, EventMetricsService metricsService) {
        this.culturalEventRepo = repository;
        this.metricsService = metricsService;
    }

    public List<CulturalEvent> findAll() {
        return metricsService.timeFindAll(culturalEventRepo::findAll);
    }

    public CulturalEvent findById(Long id) {
        return culturalEventRepo.findById(id).orElse(null);
    }

    public CulturalEvent save(CulturalEvent event) {
        boolean isNew = event.getId() == null;
        CulturalEvent saved = culturalEventRepo.save(event);
        if (isNew) {
            metricsService.recordEventCreated(event.getType());
        }
        return saved;
    }

    public void delete(Long id) {
        culturalEventRepo.deleteById(id);
        metricsService.recordEventDeleted();
    }
}
