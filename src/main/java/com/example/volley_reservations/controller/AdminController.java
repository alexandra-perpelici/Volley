package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.service.AdminService;
import com.example.volley_reservations.service.BookingPricing;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate date,
                            Model model) {
        addDashboardModel(date, model);
        return "admin-dashboard";
    }

    @GetMapping("/users/{userId}")
    public String userDetail(@PathVariable Integer userId, Model model) {
        model.addAttribute("detail", adminService.getUserDetail(userId));
        return "admin-user-detail";
    }

    @PostMapping("/reservations/{reservationId}/paid")
    public String markPaid(@PathVariable Integer reservationId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                           LocalDate date,
                           RedirectAttributes redirectAttributes) {
        LocalDate reservationDate = adminService.markReservationPaid(reservationId);
        LocalDate redirectDate = date != null ? date : reservationDate;
        redirectAttributes.addFlashAttribute("adminMessage", "Plata cash a fost marcata ca incasata.");
        return "redirect:/admin?date=" + redirectDate;
    }

    @PostMapping("/reservations/{reservationId}/attended")
    public String markAttended(@PathVariable Integer reservationId,
                               RedirectAttributes redirectAttributes) {
        adminService.markReservationAttended(reservationId);
        redirectAttributes.addFlashAttribute("adminMessage", "Rezervarea a fost marcata ca prezenta.");
        return "redirect:/admin#reservations";
    }

    @PostMapping("/reservations/{reservationId}/no-show")
    public String markNoShow(@PathVariable Integer reservationId,
                             @RequestParam(required = false) String reason,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        adminService.markReservationNoShow(reservationId, authentication.getName(), reason);
        redirectAttributes.addFlashAttribute("adminMessage", "Rezervarea a fost marcata ca neprezentare, iar utilizatorul a fost restrictionat 2 saptamani.");
        return "redirect:/admin#reservations";
    }

    @PostMapping("/users/{userId}/blacklist")
    public String blacklistUser(@PathVariable Integer userId,
                                @RequestParam(required = false) String reason,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        adminService.blacklistUser(userId, authentication.getName(), reason);
        redirectAttributes.addFlashAttribute("adminMessage", "Utilizatorul a fost restrictionat 2 saptamani.");
        return "redirect:/admin#users";
    }

    @PostMapping("/blacklist/{blacklistId}/remove")
    public String removeBlacklist(@PathVariable Integer blacklistId,
                                  RedirectAttributes redirectAttributes) {
        adminService.revokeBlacklist(blacklistId);
        redirectAttributes.addFlashAttribute("adminMessage", "Restrictia a fost ridicata.");
        return "redirect:/admin#blacklist";
    }

    private void addDashboardModel(LocalDate requestedDate, Model model) {
        LocalDate selectedDate = requestedDate != null ? requestedDate : LocalDate.now();
        List<Reservation> reservations = adminService.getReservationsForDate(selectedDate);
        long paidCount = reservations.stream()
                .filter(this::isPaymentConfirmed)
                .count();
        int selectedTotal = reservations.stream()
                .mapToInt(reservation -> BookingPricing.priceForSlot(reservation.getReservation_time()))
                .sum();
        int paidTotal = reservations.stream()
                .filter(this::isPaymentConfirmed)
                .mapToInt(reservation -> BookingPricing.priceForSlot(reservation.getReservation_time()))
                .sum();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedDateIso", selectedDate.toString());
        model.addAttribute("adminDays", buildAdminDays(selectedDate));
        model.addAttribute("dayReservations", reservations);
        model.addAttribute("reservationCount", reservations.size());
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("pendingCount", reservations.size() - paidCount);
        model.addAttribute("selectedTotal", selectedTotal);
        model.addAttribute("paidTotal", paidTotal);
        model.addAttribute("slotPrice", BookingPricing.defaultSlotPriceRon());
    }

    private List<Map<String, Object>> buildAdminDays(LocalDate selectedDate) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        if (selectedDate.isBefore(today)) {
            start = selectedDate;
        } else if (selectedDate.isAfter(today.plusDays(6))) {
            start = selectedDate.minusDays(3);
        } else {
            start = today;
        }

        return IntStream.range(0, 10)
                .mapToObj(start::plusDays)
                .map(day -> Map.<String, Object>of(
                        "date", day,
                        "name", dayName(day, today),
                        "label", "%02d.%02d".formatted(day.getDayOfMonth(), day.getMonthValue()),
                        "selected", day.equals(selectedDate)
                ))
                .toList();
    }

    private String dayName(LocalDate day, LocalDate today) {
        if (day.equals(today)) {
            return "Azi";
        }
        if (day.equals(today.plusDays(1))) {
            return "Maine";
        }

        return switch (day.getDayOfWeek()) {
            case MONDAY -> "Luni";
            case TUESDAY -> "Marti";
            case WEDNESDAY -> "Miercuri";
            case THURSDAY -> "Joi";
            case FRIDAY -> "Vineri";
            case SATURDAY -> "Sambata";
            case SUNDAY -> "Duminica";
        };
    }

    private boolean isPaymentConfirmed(Reservation reservation) {
        return reservation.getPayment() != null
                && Payment.STATUS_CONFIRMED.equals(reservation.getPayment().getStatus());
    }
}
