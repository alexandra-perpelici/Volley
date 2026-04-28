package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.event.ReservationNotificationEvent;
import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationService reservationService,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment createPendingPayment(User user, List<TemporaryReservation> cart, String paymentMethod) {
        return createPayment(user, cart, normalizePaymentMethod(paymentMethod), Payment.STATUS_PENDING);
    }

    @Transactional
    public Payment createCashAtFieldReservation(User user, List<TemporaryReservation> cart) {
        return createPayment(user, cart, Payment.METHOD_CASH_AT_FIELD, Payment.STATUS_PENDING);
    }

    private Payment createPayment(User user, List<TemporaryReservation> cart, String paymentMethod, String status) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cosul este gol.");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(status);
        payment.setAmountLei(BookingPricing.totalPriceRon(cart));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setConfirmationToken(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);
        List<Reservation> createdReservations = new ArrayList<>();

        for (TemporaryReservation temp : cart) {
            ReservationRequest request = new ReservationRequest();
            request.setUser_id(user.getUser_id());
            request.setReservation_date(temp.getDate());
            request.setReservation_time(temp.getTime());
            request.setField_number(temp.getFieldNumber());

            createdReservations.add(reservationService.createReservation(request, savedPayment));
        }

        publishReservationNotification(user, savedPayment, createdReservations);
        return savedPayment;
    }

    @Transactional(readOnly = true)
    public Payment findByToken(String confirmationToken) {
        return paymentRepository.findByConfirmationToken(confirmationToken)
                .orElseThrow(() -> new EntityNotFoundException("Plata nu a fost gasita"));
    }

    @Transactional
    public Payment confirmPayment(String confirmationToken) {
        Payment payment = findByToken(confirmationToken);
        if (!Payment.STATUS_CONFIRMED.equals(payment.getStatus())) {
            payment.setStatus(Payment.STATUS_CONFIRMED);
            payment.setConfirmedAt(LocalDateTime.now());
        }
        return payment;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (Payment.METHOD_CASH_AT_FIELD.equals(paymentMethod)
                || Payment.METHOD_CASH_AT_REGISTRY.equals(paymentMethod)
                || Payment.METHOD_CARD.equals(paymentMethod)) {
            return paymentMethod;
        }

        throw new IllegalArgumentException("Alege plata cash la teren.");
    }

    private void publishReservationNotification(User user, Payment payment, List<Reservation> reservations) {
        List<ReservationNotificationEvent.ReservationNotificationSlot> slots = reservations.stream()
                .map(reservation -> new ReservationNotificationEvent.ReservationNotificationSlot(
                        reservation.getReservation_date(),
                        reservation.getReservation_time(),
                        reservation.getField_number()
                ))
                .collect(Collectors.toList());

        eventPublisher.publishEvent(new ReservationNotificationEvent(user.getUsername(), payment.getAmountLei(), slots));
    }
}
