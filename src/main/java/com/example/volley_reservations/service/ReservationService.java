package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.event.ReservationNotificationEvent;
import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.repository.ReservationRepository;
import com.example.volley_reservations.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class ReservationService {
    private static final Duration RESERVED_SLOT_CACHE_TTL = Duration.ofSeconds(15);

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentMap<ReservedSlotCacheKey, ReservedSlotCacheEntry> reservedSlotCache = new ConcurrentHashMap<>();

    public ReservationService(ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public boolean isReservedField1(String key)
    {
        String[] parts = key.split(" ", 2);
        String datePart = parts[0];
        String hourPart = parts[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate localDate = LocalDate.parse(datePart, formatter);

        Optional<Reservation> reservation = reservationRepository.findReservation(localDate, hourPart, 1);
        return reservation.isPresent();
    }

    public boolean isReservedField2(String key)
    {
        String[] parts = key.split(" ",2);
        String datePart = parts[0];
        String hourPart = parts[1];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate localDate = LocalDate.parse(datePart, formatter);


        Optional<Reservation> reservation = reservationRepository.findReservation(localDate, hourPart, 2);
        return reservation.isPresent();
    }

    public Set<String> findReservedSlotKeys(int fieldNumber, LocalDate startDate, LocalDate endDate) {
        ReservedSlotCacheKey cacheKey = new ReservedSlotCacheKey(fieldNumber, startDate, endDate);
        ReservedSlotCacheEntry cachedEntry = reservedSlotCache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.slotKeys();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        List<Reservation> reservations = reservationRepository.findReservationsForFieldBetweenDates(fieldNumber, startDate, endDate);

        Set<String> slotKeys = reservations.stream()
                .flatMap(reservation -> toVisibleSlotKeys(reservation, formatter).stream())
                .collect(Collectors.toSet());
        Set<String> immutableSlotKeys = Set.copyOf(slotKeys);
        reservedSlotCache.put(cacheKey, new ReservedSlotCacheEntry(immutableSlotKeys));
        return immutableSlotKeys;
    }

    public void deleteReservation(int reservationId) {
        reservationRepository.deleteById(reservationId);
        reservedSlotCache.clear();
    }


    @Scheduled(cron = "0 10 3 * * *")
    public void deleteOldReservations(){
        LocalDate today = LocalDate.now();
        reservationRepository.deleteAllBeforeToday(today);
        reservedSlotCache.clear();

    }

    public String createNewReservation(ReservationRequest request)
    {
        try {
            Reservation reservation = createReservation(request, null);
            eventPublisher.publishEvent(new ReservationNotificationEvent(
                    reservation.getUser().getUsername(),
                    BookingPricing.priceForSlot(reservation.getReservation_time()),
                    List.of(new ReservationNotificationEvent.ReservationNotificationSlot(
                            reservation.getReservation_date(),
                            reservation.getReservation_time(),
                            reservation.getField_number()
                    ))
            ));
            return "Rezervarea a fost creata.";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return exception.getMessage();
        }
    }

    public Reservation createReservation(ReservationRequest request, Payment payment) {
        validateReservation(request);
        Reservation reservation = new Reservation();
        reservation.setReservation_date(request.getReservation_date());
        reservation.setReservation_time(request.getReservation_time());
        reservation.setUser(userRepository.findUserById(request.getUser_id()));
        reservation.setField_number(request.getField_number());
        reservation.setPayment(payment);

        Reservation savedReservation = reservationRepository.save(reservation);
        reservedSlotCache.clear();
        return savedReservation;
    }

    private void validateReservation(ReservationRequest request) {
        if (request.getField_number() < 1 || request.getField_number() > 2) {
            throw new IllegalArgumentException("Teren invalid.");
        }

        if (reservationRepository.existsSlot(request.getReservation_date(), request.getReservation_time(), request.getField_number())
                || overlapsExistingReservation(request)) {
            throw new IllegalStateException("Exista deja o rezervare pentru acest teren si interval.");
        }
    }

    private boolean overlapsExistingReservation(ReservationRequest request) {
        TimeRange requestedRange = parseTimeRange(request.getReservation_time());
        if (requestedRange == null) {
            return false;
        }

        return reservationRepository.findReservationsForFieldBetweenDates(
                        request.getField_number(),
                        request.getReservation_date(),
                        request.getReservation_date()
                ).stream()
                .map(reservation -> parseTimeRange(reservation.getReservation_time()))
                .filter(Objects::nonNull)
                .anyMatch(existingRange -> requestedRange.overlaps(existingRange));
    }

    private List<String> toVisibleSlotKeys(Reservation reservation, DateTimeFormatter formatter) {
        TimeRange reservationRange = parseTimeRange(reservation.getReservation_time());
        String day = reservation.getReservation_date().format(formatter);
        if (reservationRange == null) {
            return List.of(day + " " + reservation.getReservation_time());
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

    private record ReservedSlotCacheKey(int fieldNumber, LocalDate startDate, LocalDate endDate) {
    }

    private record ReservedSlotCacheEntry(Set<String> slotKeys, long expiresAtMillis) {
        ReservedSlotCacheEntry(Set<String> slotKeys) {
            this(slotKeys, System.currentTimeMillis() + RESERVED_SLOT_CACHE_TTL.toMillis());
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

}
