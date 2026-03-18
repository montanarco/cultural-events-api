package com.epam.java_learning_epam.repository;

import com.epam.java_learning_epam.model.CulturalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CulturalEventRepository extends JpaRepository<CulturalEvent, Long> {
}