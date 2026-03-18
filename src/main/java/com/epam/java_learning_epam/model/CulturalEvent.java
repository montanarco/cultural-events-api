package com.epam.java_learning_epam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "cultural_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CulturalEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private Date date;

    private String location;
    private String address;
    private String description;

    // Getters and setters
}