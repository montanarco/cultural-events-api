package com.epam.java_learning_epam.controller;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.service.CulturalEventService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/events")
public class CulturalEventController {
    private final CulturalEventService culturalEventservice;

    public CulturalEventController(CulturalEventService service) {
        this.culturalEventservice = service;
    }

    @GetMapping
    public List<CulturalEvent> getAll() { return culturalEventservice.findAll(); }

    @GetMapping("/{id}")
    public CulturalEvent getById(@PathVariable Long id) { return culturalEventservice.findById(id); }

    @PostMapping
    public CulturalEvent create(@RequestBody CulturalEvent event) { return culturalEventservice.save(event); }

    @PutMapping("/{id}")
    public CulturalEvent update(@PathVariable Long id, @RequestBody CulturalEvent event) {
        event.setId(id);
        return culturalEventservice.save(event);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { culturalEventservice.delete(id); }
}