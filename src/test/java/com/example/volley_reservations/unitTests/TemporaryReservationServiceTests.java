package com.example.volley_reservations.unitTests;

import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.service.TemporaryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryReservationServiceTests {

    private TemporaryReservationService service;
    private final int USER_ID = 1;
    private final LocalDate DATE = LocalDate.of(2026, 1, 28);
    private final String TIME = "10:00";
    private final int FIELD = 1;

    @BeforeEach
    void setUp() {

        service = new TemporaryReservationService();
    }

    @Test
    void testAddTemporaryReservation_Success() {
        boolean result = service.addTemporaryReservation(USER_ID, DATE, TIME, FIELD);
        assertTrue(result, "Should successfully add a new reservation.");
        assertEquals(1, service.getUserReservations(USER_ID).size(), "User should have 1 item in cart.");
    }

    @Test
    void testAddTemporaryReservation_FailsWhenAlreadyLocked() {
        service.addTemporaryReservation(USER_ID, DATE, TIME, FIELD);
        boolean result = service.addTemporaryReservation(2, DATE, TIME, FIELD);
        assertFalse(result, "Should fail because the slot is already locked by another user.");
    }
}