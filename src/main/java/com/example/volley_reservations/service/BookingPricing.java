package com.example.volley_reservations.service;

import com.example.volley_reservations.model.TemporaryReservation;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public final class BookingPricing {

    public static final int PRICE_PER_HOUR_RON = 40;
    private static final int DEFAULT_SLOT_MINUTES = 60;

    private BookingPricing() {
    }

    public static int priceForSlot(String timeSlot) {
        int minutes = parseSlotMinutes(timeSlot);
        return (minutes * PRICE_PER_HOUR_RON) / 60;
    }

    public static int defaultSlotPriceRon() {
        return (DEFAULT_SLOT_MINUTES * PRICE_PER_HOUR_RON) / 60;
    }

    public static int totalPriceRon(List<TemporaryReservation> reservations) {
        return reservations.stream()
                .mapToInt(reservation -> priceForSlot(reservation.getTime()))
                .sum();
    }

    private static int parseSlotMinutes(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains(" - ")) {
            return DEFAULT_SLOT_MINUTES;
        }

        try {
            String[] parts = timeSlot.split(" - ");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            long minutes = Duration.between(start, end).toMinutes();
            return minutes > 0 ? (int) minutes : DEFAULT_SLOT_MINUTES;
        } catch (RuntimeException exception) {
            return DEFAULT_SLOT_MINUTES;
        }
    }
}
