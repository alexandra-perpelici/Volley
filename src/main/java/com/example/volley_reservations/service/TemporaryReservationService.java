package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TemporaryReservationService {

    // userId -> list of temporary reservations
    private final Map<Integer, List<TemporaryReservation>> temporaryReservations = new ConcurrentHashMap<>();

    // key = "date time field" -> userId who locked it
    private final Map<String, Integer> lockedReservations = new ConcurrentHashMap<>();

    private static final int LOCK_MINUTES = 10;

    // Add a temporary reservation
    public synchronized boolean addTemporaryReservation(int userId, LocalDate date, String time, int fieldNumber) {
        if (fieldNumber < 1 || fieldNumber > 2) {
            return false;
        }

        cleanupExpiredReservations(); // remove expired before adding

        String key = generateKey(date, time, fieldNumber);

        if (lockedReservations.containsKey(key) || overlapsTemporaryReservation(date, time, fieldNumber)) {
            return false; // slot locked by another user
        }

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        TemporaryReservation temp = new TemporaryReservation(date, time, fieldNumber, expireAt);

        temporaryReservations.computeIfAbsent(userId, k -> new ArrayList<>()).add(temp);
        lockedReservations.put(key, userId);

        return true;
    }

    // Get all temporary reservations of a user
    public synchronized List<TemporaryReservation> getUserReservations(int userId) {
        cleanupExpiredReservations();

        return new ArrayList<>(temporaryReservations.getOrDefault(userId, new ArrayList<>()));
    }

    // Remove a reservation from cart
    public synchronized void removeReservation(int userId, TemporaryReservation temp) {
        List<TemporaryReservation> userRes = temporaryReservations.get(userId);
        if (userRes != null) {
            userRes.remove(temp);
            String key = generateKey(temp.getDate(), temp.getTime(), temp.getFieldNumber());
            lockedReservations.remove(key);
        }
    }

    // Confirm reservations and persist them to DB
    public synchronized void confirmReservations(int userId, ReservationService reservationService, User user) {
        List<TemporaryReservation> userRes = temporaryReservations.get(userId);
        if (userRes == null || userRes.isEmpty()) return;

        for (TemporaryReservation temp : userRes) {
            ReservationRequest request = new ReservationRequest();
            request.setUser_id(userId);
            request.setReservation_date(temp.getDate());
            request.setReservation_time(temp.getTime());
            request.setField_number(temp.getFieldNumber());

            reservationService.createNewReservation(request);

            String key = generateKey(temp.getDate(), temp.getTime(), temp.getFieldNumber());
            lockedReservations.remove(key);
        }

        temporaryReservations.remove(userId);
    }

    public synchronized void clearUserReservations(int userId) {
        List<TemporaryReservation> userRes = temporaryReservations.remove(userId);
        if (userRes == null) {
            return;
        }

        for (TemporaryReservation temp : userRes) {
            lockedReservations.remove(generateKey(temp.getDate(), temp.getTime(), temp.getFieldNumber()));
        }
    }

    // Cleanup expired reservations
    private synchronized void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        for (Iterator<Map.Entry<Integer, List<TemporaryReservation>>> it = temporaryReservations.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, List<TemporaryReservation>> entry = it.next();
            List<TemporaryReservation> resList = entry.getValue();
            resList.removeIf(res -> {
                if (res.getExpireAt().isBefore(now)) {
                    String key = generateKey(res.getDate(), res.getTime(), res.getFieldNumber());
                    lockedReservations.remove(key);
                    return true;
                }
                return false;
            });

            if (resList.isEmpty()) it.remove();
        }
    }

    private String generateKey(LocalDate date, String time, int fieldNumber) {
        return date.toString() + " " + time + " " + fieldNumber;
    }

    public synchronized boolean isTemporarilyReserved(LocalDate date, String time, int fieldNumber) {
        cleanupExpiredReservations(); // remove expired

        String key = generateKey(date, time, fieldNumber);
        return lockedReservations.containsKey(key);
    }

    public synchronized Set<String> findTemporaryReservedSlotKeys(int fieldNumber, LocalDate startDate, LocalDate endDate) {
        cleanupExpiredReservations();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Set<String> reservedKeys = new HashSet<>();

        for (List<TemporaryReservation> reservations : temporaryReservations.values()) {
            for (TemporaryReservation reservation : reservations) {
                LocalDate date = reservation.getDate();
                boolean inRange = !date.isBefore(startDate) && !date.isAfter(endDate);
                if (inRange && reservation.getFieldNumber() == fieldNumber) {
                    reservedKeys.addAll(toVisibleSlotKeys(reservation, formatter));
                }
            }
        }

        return reservedKeys;
    }

    private boolean overlapsTemporaryReservation(LocalDate date, String time, int fieldNumber) {
        TimeRange requestedRange = parseTimeRange(time);
        if (requestedRange == null) {
            return false;
        }

        return temporaryReservations.values().stream()
                .flatMap(List::stream)
                .filter(reservation -> reservation.getFieldNumber() == fieldNumber && reservation.getDate().equals(date))
                .map(reservation -> parseTimeRange(reservation.getTime()))
                .filter(Objects::nonNull)
                .anyMatch(requestedRange::overlaps);
    }

    private List<String> toVisibleSlotKeys(TemporaryReservation reservation, DateTimeFormatter formatter) {
        TimeRange reservationRange = parseTimeRange(reservation.getTime());
        String day = reservation.getDate().format(formatter);
        if (reservationRange == null) {
            return List.of(day + " " + reservation.getTime());
        }

        List<String> keys = new ArrayList<>();
        LocalTime cursor = LocalTime.of(8, 0);
        LocalTime scheduleEnd = LocalTime.of(20, 0);
        while (cursor.isBefore(scheduleEnd)) {
            LocalTime next = cursor.plusHours(1);
            TimeRange visibleSlot = new TimeRange(cursor, next);
            if (visibleSlot.overlaps(reservationRange)) {
                keys.add(day + " " + cursor + " - " + next);
            }
            cursor = next;
        }
        return keys;
    }

    private TimeRange parseTimeRange(String value) {
        if (value == null || !value.contains(" - ")) {
            return null;
        }

        try {
            String[] parts = value.split(" - ");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            if (!start.isBefore(end)) {
                return null;
            }
            return new TimeRange(start, end);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private record TimeRange(LocalTime start, LocalTime end) {
        boolean overlaps(TimeRange other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }
}
