package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        cleanupExpiredReservations(); // remove expired before adding

        String key = generateKey(date, time, fieldNumber);

        if (lockedReservations.containsKey(key)) {
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
}
