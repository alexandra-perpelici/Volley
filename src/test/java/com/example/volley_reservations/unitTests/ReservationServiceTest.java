package com.example.volley_reservations.unitTests;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.ReservationRepository;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void testIsReservedField1_ReturnsTrueWhenFound() {
        String key = "28-01-2026 10:00";
        LocalDate parsedDate = LocalDate.of(2026, 1, 28);
        String hour = "10:00";
        when(reservationRepository.findReservation(parsedDate, hour, 1))
                .thenReturn(Optional.of(new Reservation()));

        boolean result = reservationService.isReservedField1(key);
        assertTrue(result, "Should return true if the repository finds a reservation");
        verify(reservationRepository, times(1)).findReservation(parsedDate, hour, 1);
    }

    @Test
    void testCreateNewReservation_FailsIfSlotAlreadyExists() {
        ReservationRequest request = new ReservationRequest();
        request.setUser_id(1);
        request.setReservation_date(LocalDate.of(2026, 1, 28));
        request.setReservation_time("14:00");
        request.setField_number(1);

        when(reservationRepository.existsSlot(request.getReservation_date(), "14:00", 1))
                .thenReturn(true);

        String response = reservationService.createNewReservation(request);
        assertEquals("Reservation already exists for this court and time slot!", response);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }
}
