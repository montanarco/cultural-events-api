package com.epam.java_learning_epam.controller;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.model.EventType;
import com.epam.java_learning_epam.service.CulturalEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests — web behaviour and security rules only.
 *
 * Spring Boot 4.0 removed @WebMvcTest. We use @SpringBootTest (full context)
 * and set up MockMvc manually via MockMvcBuilders.webAppContextSetup().
 *
 * .apply(springSecurity()) wires the Spring Security filter chain into MockMvc
 * so that @WithMockUser and authorization rules are evaluated on every request.
 *
 * @MockitoBean replaces CulturalEventService in the application context with a
 * Mockito mock. This isolates the controller: no real service/DB logic runs,
 * only the controller routing, request parsing, and security enforcement are tested.
 *
 * @WithMockUser injects a pre-authenticated principal directly into the
 * SecurityContext before the request is dispatched. JwtAuthFilter sees no
 * Bearer header and skips processing, leaving the mock principal intact.
 */
@SpringBootTest
class CulturalEventControllerTest {

    @Autowired
    private WebApplicationContext context;

    // Instantiated directly — ObjectMapper is not auto-configured as a bean
    // in @SpringBootTest contexts that use @MockitoBean (Spring Boot 4.x behaviour)
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Replaces the real CulturalEventService bean with a Mockito mock
    @MockitoBean
    private CulturalEventService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ------------------------------------------------------------------
    // GET /events
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "USER")
    void getAll_asUser_returns200WithEventList() throws Exception {
        List<CulturalEvent> events = List.of(
                buildEvent(1L, "Jazz Night"),
                buildEvent(2L, "Art Show")
        );
        when(service.findAll()).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Jazz Night"))
                .andExpect(jsonPath("$[1].name").value("Art Show"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_asAdmin_returns200() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------
    // GET /events/{id}
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "USER")
    void getById_asUser_returns200_whenEventExists() throws Exception {
        CulturalEvent event = buildEvent(1L, "Jazz Night");
        when(service.findById(1L)).thenReturn(event);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jazz Night"))
                .andExpect(jsonPath("$.type").value("MUSIC"));
    }

    @Test
    void getById_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/events/1"))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------
    // POST /events
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns200WithCreatedEvent() throws Exception {
        CulturalEvent input = buildEvent(null, "New Event");
        CulturalEvent saved = buildEvent(10L, "New Event");
        when(service.save(any(CulturalEvent.class))).thenReturn(saved);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New Event"));

        verify(service).save(any(CulturalEvent.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEvent(null, "Blocked"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEvent(null, "Blocked"))))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------
    // PUT /events/{id}
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200WithUpdatedEvent() throws Exception {
        CulturalEvent input = buildEvent(null, "Updated Name");
        CulturalEvent saved = buildEvent(1L, "Updated Name");
        when(service.save(any(CulturalEvent.class))).thenReturn(saved);

        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEvent(null, "Blocked"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // DELETE /events/{id}
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns200() throws Exception {
        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isOk());

        verify(service).delete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isForbidden());
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
