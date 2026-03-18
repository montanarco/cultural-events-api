package com.epam.java_learning_epam.repository;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.model.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests.
 *
 * Spring Boot 4.0 removed @DataJpaTest, so we use @SpringBootTest which loads
 * the full application context (including Flyway, which seeds 4 events on startup).
 *
 * @Transactional ensures each test method runs inside a transaction that is
 * rolled back automatically after the test completes. This provides test
 * isolation: any data inserted during a test is never committed to H2.
 *
 * Note: the 4 rows seeded by Flyway V2 are committed before tests run and are
 * visible in every test. Assertions use relative counts (before + delta) or
 * greaterThanOrEqualTo to stay independent of the seed data.
 */
@SpringBootTest
@Transactional
class CulturalEventRepositoryTest {

    @Autowired
    private CulturalEventRepository repository;

    // ------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------

    @Test
    void save_persistsNewEvent_andAssignsGeneratedId() {
        CulturalEvent event = buildEvent(null, "Jazz Night", EventType.MUSIC);

        CulturalEvent saved = repository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Jazz Night");
        assertThat(saved.getType()).isEqualTo(EventType.MUSIC);
    }

    @Test
    void save_updatesExistingEvent_withouCreatingDuplicate() {
        CulturalEvent saved = repository.save(buildEvent(null, "Original Name", EventType.ART));
        long countAfterInsert = repository.count();

        saved.setName("Updated Name");
        repository.save(saved);

        assertThat(repository.count()).isEqualTo(countAfterInsert); // no extra row
        assertThat(repository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(CulturalEvent::getName)
                .isEqualTo("Updated Name");
    }

    // ------------------------------------------------------------------
    // Find
    // ------------------------------------------------------------------

    @Test
    void findById_returnsEvent_whenExists() {
        CulturalEvent saved = repository.save(buildEvent(null, "Art Show", EventType.ART));

        Optional<CulturalEvent> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Art Show");
        assertThat(result.get().getType()).isEqualTo(EventType.ART);
    }

    @Test
    void findById_returnsEmpty_whenIdDoesNotExist() {
        Optional<CulturalEvent> result = repository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllEvents_includingSeedData() {
        repository.save(buildEvent(null, "New Event A", EventType.MUSIC));
        repository.save(buildEvent(null, "New Event B", EventType.SPORTS));

        List<CulturalEvent> all = repository.findAll();

        // Seed data (4 rows) + 2 new = at least 6
        assertThat(all).hasSizeGreaterThanOrEqualTo(6);
        assertThat(all).extracting(CulturalEvent::getName)
                .contains("New Event A", "New Event B");
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    void deleteById_removesOnlyTheTargetedEvent() {
        CulturalEvent saved = repository.save(buildEvent(null, "Theater Night", EventType.THEATER));
        Long id = saved.getId();
        long countBefore = repository.count();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.count()).isEqualTo(countBefore - 1);
    }

    // ------------------------------------------------------------------
    // Count
    // ------------------------------------------------------------------

    @Test
    void count_incrementsByOneAfterInsert() {
        long before = repository.count();

        repository.save(buildEvent(null, "Camping Trip", EventType.CAMPING));

        assertThat(repository.count()).isEqualTo(before + 1);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private CulturalEvent buildEvent(Long id, String name, EventType type) {
        return CulturalEvent.builder()
                .id(id)
                .name(name)
                .type(type)
                .date(new Date())
                .location("Test Location")
                .address("123 Test St")
                .description("Test description")
                .build();
    }
}
