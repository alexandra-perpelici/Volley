package com.example.volley_reservations.event;

import java.time.LocalDate;
import java.util.List;

public record ReservationNotificationEvent(
        String username,
        int amountLei,
        List<ReservationNotificationSlot> slots
) {

    public record ReservationNotificationSlot(
            LocalDate date,
            String time,
            int fieldNumber
    ) {
    }
}
