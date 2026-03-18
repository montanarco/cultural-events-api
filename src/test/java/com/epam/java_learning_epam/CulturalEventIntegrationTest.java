package com.epam.java_learning_epam;

import com.epam.java_learning_epam.model.CulturalEvent;
import com.epam.java_learning_epam.model.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests.
 *
 * @SpringBootTest loads the complete application context:
 *  - Flyway runs V1 (schema) and V2 (seed: 4 events) on startup.
 *  - Real JwtService, SecurityConfig, filter chain, service, and repository are active.
 *  - H2 in-memory database is used (same config as dev profile).
 *
 * MockMvc is set up via MockMvcBuilders.webAppContextSetup() (required in Boot 4 —
 * @AutoConfigureMockMvc was removed). .apply(springSecurity()) activates the full
 * Spring Security filter chain for each request.
 *
 * Authentication: real JWT tokens are obtained from POST /auth/login using the
 * in-memory users configured in SecurityConfig (admin/admin123, user/user123).
 *
 * Test isolation note: @SpringBootTest does NOT roll back transactions between tests.
 * Write tests (POST, PUT, DELETE) create their own data and do not rely on shared state.
 * GET tests use greaterThanOrEqualTo to tolerate data inserted by other tests.
 */
@SpringBootTest
class CulturalEventIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    // Instantiated directly — ObjectMapper is not auto-configured as a bean
    // in @SpringBootTest contexts in Spring Boot 4.x
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");
    }

    // ------------------------------------------------------------------
    // POST /auth/login
    // ------------------------------------------------------------------

    @Test
    void login_withValidAdminCredentials_returnsJwtToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty());
    }

    @Test
    void login_withValidUserCredentials_returnsJwtToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "user", "password", "user123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_returns4xx() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "wrongpassword"))))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------
    // GET /events
    // ------------------------------------------------------------------

    @Test
    void getAll_withAdminToken_returns200AndAtLeastSeedData() throws Exception {
        mockMvc.perform(get("/events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    void getAll_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/events")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_withNoToken_returns4xx() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getAll_withInvalidToken_returns4xx() throws Exception {
        mockMvc.perform(get("/events")
                        .header("Authorization", "Bearer this.is.not.valid"))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------
    // GET /events/{id}
    // ------------------------------------------------------------------

    @Test
    void getById_withAdminToken_returns200ForSeededEvent() throws Exception {
        // id=1 is inserted by V2 Flyway migration
        mockMvc.perform(get("/events/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/events/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // POST /events
    // ------------------------------------------------------------------

    @Test
    void create_withAdminToken_persistsEventAndReturnsIt() throws Exception {
        CulturalEvent event = buildEvent("Integration Test Concert", EventType.MUSIC);

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Integration Test Concert"))
                .andExpect(jsonPath("$.type").value("MUSIC"));
    }

    @Test
    void create_withUserToken_returns403() throws Exception {
        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildEvent("Blocked Event", EventType.ART))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // PUT /events/{id}
    // ------------------------------------------------------------------

    @Test
    void update_withAdminToken_returns200AndUpdatedName() throws Exception {
        // Create a dedicated event so this test is self-contained
        CulturalEvent created = createEventAsAdmin("Event to Update", EventType.THEATER);
        created.setName("Updated Name");

        mockMvc.perform(put("/events/" + created.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void update_withUserToken_returns403() throws Exception {
        mockMvc.perform(put("/events/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildEvent("Blocked Update", EventType.SPORTS))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // DELETE /events/{id}
    // ------------------------------------------------------------------

    @Test
    void delete_withAdminToken_returns200() throws Exception {
        // Create a dedicated event so the delete is self-contained
        CulturalEvent created = createEventAsAdmin("Event to Delete", EventType.CAMPING);

        mockMvc.perform(delete("/events/" + created.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void delete_withUserToken_returns403() throws Exception {
        mockMvc.perform(delete("/events/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Calls POST /auth/login and returns the JWT access_token string.
     */
    private String obtainToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("access_token").asText();
    }

    /**
     * Creates an event via POST /events as admin and returns the persisted entity
     * with the server-assigned id. Used to set up isolated data for write tests.
     */
    private CulturalEvent createEventAsAdmin(String name, EventType type) throws Exception {
        CulturalEvent event = buildEvent(name, type);

        String response = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, CulturalEvent.class);
    }

    private CulturalEvent buildEvent(String name, EventType type) {
        return CulturalEvent.builder()
                .name(name)
                .type(type)
                .date(new Date())
                .location("Test Venue")
                .address("42 Test Street")
                .description("Created by integration test")
                .build();
    }
}
