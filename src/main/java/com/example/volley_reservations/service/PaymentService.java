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

    private static final int PRICE_PER_RESERVATION_LEI = 20;

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
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPaymentMethod(normalizePaymentMethod(paymentMethod));
        payment.setStatus(Payment.STATUS_PENDING);
        payment.setAmountLei(cart.size() * PRICE_PER_RESERVATION_LEI);
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
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
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
        if (Payment.METHOD_CARD.equals(paymentMethod) || Payment.METHOD_CASH_AT_REGISTRY.equals(paymentMethod)) {
            return paymentMethod;
        }

        throw new IllegalArgumentException("Choose card or cash at registry.");
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
