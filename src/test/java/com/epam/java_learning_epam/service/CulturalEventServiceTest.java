package com.epam.java_learning_epam.service;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.model.EventType;
import com.epam.java_learning_epam.monitor.EventMetricsService;
import com.epam.java_learning_epam.repository.CulturalEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CulturalEventService.
 *
 * No Spring context is loaded — plain Mockito with @ExtendWith(MockitoExtension.class).
 * All dependencies (repository, metricsService) are mocked, so each test exercises
 * only the service logic in isolation.
 *
 * Key pattern: metricsService.timeFindAll(Supplier) wraps the actual repository call.
 * The mock is configured to execute the supplier so the repository mock is also exercised.
 */
@ExtendWith(MockitoExtension.class)
class CulturalEventServiceTest {

    @Mock
    private CulturalEventRepository repository;

    @Mock
    private EventMetricsService metricsService;

    @InjectMocks
    private CulturalEventService service;

    // ------------------------------------------------------------------
    // findAll
    // ------------------------------------------------------------------

    @Test
    void findAll_delegatesToTimerAndReturnsEvents() {
        List<CulturalEvent> events = List.of(
                buildEvent(1L, "Jazz Night"),
                buildEvent(2L, "Art Show")
        );
        when(repository.findAll()).thenReturn(events);
        // timeFindAll receives a Supplier — execute it to simulate the Timer wrapper
        when(metricsService.timeFindAll(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());

        List<CulturalEvent> result = service.findAll();

        assertThat(result).hasSize(2);
        verify(metricsService).timeFindAll(any());
        verify(repository).findAll();
    }

    // ------------------------------------------------------------------
    // findById
    // ------------------------------------------------------------------

    @Test
    void findById_returnsEvent_whenFound() {
        CulturalEvent event = buildEvent(1L, "Jazz Night");
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        CulturalEvent result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Jazz Night");
    }

    @Test
    void findById_returnsNull_whenNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        CulturalEvent result = service.findById(999L);

        assertThat(result).isNull();
    }

    // ------------------------------------------------------------------
    // save — new event
    // ------------------------------------------------------------------

    @Test
    void save_newEvent_recordsCreatedMetricWithType() {
        CulturalEvent newEvent = buildEvent(null, "New Event"); // null id = new
        newEvent.setType(EventType.MUSIC);
        CulturalEvent savedEvent = buildEvent(10L, "New Event");
        when(repository.save(newEvent)).thenReturn(savedEvent);

        CulturalEvent result = service.save(newEvent);

        assertThat(result.getId()).isEqualTo(10L);
        verify(metricsService).recordEventCreated(EventType.MUSIC);
    }

    @Test
    void save_newEvent_withNullType_stillRecordsMetric() {
        CulturalEvent newEvent = buildEvent(null, "No Type Event");
        newEvent.setType(null);
        when(repository.save(newEvent)).thenReturn(buildEvent(11L, "No Type Event"));

        service.save(newEvent);

        // recordEventCreated is still called — with null type, the service passes null
        verify(metricsService).recordEventCreated(null);
    }

    // ------------------------------------------------------------------
    // save — existing event (update)
    // ------------------------------------------------------------------

    @Test
    void save_existingEvent_doesNotRecordCreateMetric() {
        CulturalEvent existing = buildEvent(1L, "Existing Event"); // non-null id = update
        when(repository.save(existing)).thenReturn(existing);

        service.save(existing);

        verify(metricsService, never()).recordEventCreated(any());
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_callsRepositoryAndRecordsDeletedMetric() {
        service.delete(5L);

        verify(repository).deleteById(5L);
        verify(metricsService).recordEventDeleted();
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private CulturalEvent buildEvent(Long id, String name) {
        return CulturalEvent.builder()
                .id(id)
                .name(name)
                .type(EventType.MUSIC)
                .build();
    }
}
