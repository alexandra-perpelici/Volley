package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.AdminDashboardStats;
import com.example.volley_reservations.dto.AdminUserDetail;
import com.example.volley_reservations.dto.AdminUserSummary;
import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.UserBlacklistEntry;
import com.example.volley_reservations.repository.PaymentRepository;
import com.example.volley_reservations.repository.ReservationRepository;
import com.example.volley_reservations.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserBlacklistService blacklistService;

    public AdminService(UserRepository userRepository,
                        ReservationRepository reservationRepository,
                        PaymentRepository paymentRepository,
                        UserBlacklistService blacklistService) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.blacklistService = blacklistService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);
        YearMonth currentMonth = YearMonth.from(today);

        return new AdminDashboardStats(
                userRepository.count(),
                blacklistService.countActiveEntries(),
                reservationRepository.countReservationsOnDate(today),
                reservationRepository.countReservationsBetweenDates(today, weekEnd),
                reservationRepository.countReservationsByStatusBetweenDates(
                        Reservation.STATUS_NO_SHOW,
                        currentMonth.atDay(1),
                        currentMonth.atEndOfMonth()
                ),
                paymentRepository.countByStatus(Payment.STATUS_PENDING),
                paymentRepository.sumConfirmedAmountLei()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummary> getUserSummaries() {
        LocalDate today = LocalDate.now();
        return userRepository.findAll().stream()
                .map(user -> new AdminUserSummary(
                        user,
                        reservationRepository.countReservationsForUser(user.getUser_id()),
                        reservationRepository.countUpcomingReservationsForUser(user.getUser_id(), today),
                        reservationRepository.countNoShowsForUser(user.getUser_id()),
                        blacklistService.findActiveForUser(user).orElse(null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetail getUserDetail(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizatorul nu a fost gasit"));
        LocalDate today = LocalDate.now();
        AdminUserSummary summary = new AdminUserSummary(
                user,
                reservationRepository.countReservationsForUser(user.getUser_id()),
                reservationRepository.countUpcomingReservationsForUser(user.getUser_id(), today),
                reservationRepository.countNoShowsForUser(user.getUser_id()),
                blacklistService.findActiveForUser(user).orElse(null)
        );

        return new AdminUserDetail(
                summary,
                reservationRepository.findAdminReservationsForUser(userId),
                blacklistService.findHistoryForUser(userId)
        );
    }

    @Transactional(readOnly = true)
    public List<Reservation> getRecentReservations(int limit) {
        return reservationRepository.findAdminReservations(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<UserBlacklistEntry> getActiveBlacklists() {
        return blacklistService.findActiveEntries();
    }

    @Transactional
    public void markReservationAttended(Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Rezervarea nu a fost gasita"));
        reservation.setStatus(Reservation.STATUS_ATTENDED);
    }

    @Transactional
    public void markReservationNoShow(Integer reservationId, String adminUsername, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Rezervarea nu a fost gasita"));
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new EntityNotFoundException("Administratorul nu a fost gasit"));

        reservation.setStatus(Reservation.STATUS_NO_SHOW);
        blacklistService.createTwoWeekBlacklist(reservation.getUser(), admin, reservation, reason);
    }

    @Transactional
    public void blacklistUser(Integer userId, String adminUsername, String reason) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizatorul nu a fost gasit"));
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new EntityNotFoundException("Administratorul nu a fost gasit"));

        blacklistService.createTwoWeekBlacklist(targetUser, admin, null, reason);
    }

    @Transactional
    public void revokeBlacklist(Integer blacklistId) {
        blacklistService.revokeBlacklist(blacklistId);
    }
}
