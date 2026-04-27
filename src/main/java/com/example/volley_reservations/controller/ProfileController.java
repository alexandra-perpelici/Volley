package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.ReservationRepository;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.TemporaryReservationService;
import com.example.volley_reservations.service.UserBlacklistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final TemporaryReservationService temporaryReservationService;
    private final UserBlacklistService blacklistService;

    public ProfileController(UserRepository userRepository,
                             ReservationRepository reservationRepository,
                             TemporaryReservationService temporaryReservationService,
                             UserBlacklistService blacklistService) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.temporaryReservationService = temporaryReservationService;
        this.blacklistService = blacklistService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findReservationsForUser(user.getUser_id());
        List<Reservation> upcomingReservations = reservations.stream()
                .filter(reservation -> !Reservation.STATUS_CANCELLED.equals(reservation.getStatus()))
                .filter(reservation -> !reservation.getReservation_date().isBefore(today))
                .sorted(Comparator.comparing(Reservation::getReservation_date)
                        .thenComparing(Reservation::getReservation_time))
                .toList();
        List<Reservation> reservationHistory = reservations.stream()
                .filter(reservation -> reservation.getReservation_date().isBefore(today)
                        || Reservation.STATUS_CANCELLED.equals(reservation.getStatus()))
                .limit(8)
                .toList();
        List<TemporaryReservation> cart = temporaryReservationService.getUserReservations(user.getUser_id());

        model.addAttribute("user", user);
        model.addAttribute("upcomingReservations", upcomingReservations);
        model.addAttribute("reservationHistory", reservationHistory);
        model.addAttribute("reservationCount", reservations.size());
        model.addAttribute("upcomingCount", upcomingReservations.size());
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", cart.size() * 20);
        model.addAttribute("activeBlacklist", blacklistService.findActiveForUser(user).orElse(null));
        return "profile";
    }
}
