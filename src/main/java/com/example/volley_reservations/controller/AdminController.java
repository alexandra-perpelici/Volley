package com.example.volley_reservations.controller;

import com.example.volley_reservations.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String dashboard(Model model) {
        addDashboardModel(model);
        return "admin-dashboard";
    }

    @GetMapping("/users/{userId}")
    public String userDetail(@PathVariable Integer userId, Model model) {
        model.addAttribute("detail", adminService.getUserDetail(userId));
        return "admin-user-detail";
    }

    @PostMapping("/reservations/{reservationId}/attended")
    public String markAttended(@PathVariable Integer reservationId,
                               RedirectAttributes redirectAttributes) {
        adminService.markReservationAttended(reservationId);
        redirectAttributes.addFlashAttribute("adminMessage", "Reservation marked as attended.");
        return "redirect:/admin#reservations";
    }

    @PostMapping("/reservations/{reservationId}/no-show")
    public String markNoShow(@PathVariable Integer reservationId,
                             @RequestParam(required = false) String reason,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        adminService.markReservationNoShow(reservationId, authentication.getName(), reason);
        redirectAttributes.addFlashAttribute("adminMessage", "Reservation marked as no-show and user blacklisted for 2 weeks.");
        return "redirect:/admin#reservations";
    }

    @PostMapping("/users/{userId}/blacklist")
    public String blacklistUser(@PathVariable Integer userId,
                                @RequestParam(required = false) String reason,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        adminService.blacklistUser(userId, authentication.getName(), reason);
        redirectAttributes.addFlashAttribute("adminMessage", "User blacklisted for 2 weeks.");
        return "redirect:/admin#users";
    }

    @PostMapping("/blacklist/{blacklistId}/remove")
    public String removeBlacklist(@PathVariable Integer blacklistId,
                                  RedirectAttributes redirectAttributes) {
        adminService.revokeBlacklist(blacklistId);
        redirectAttributes.addFlashAttribute("adminMessage", "Blacklist entry removed.");
        return "redirect:/admin#blacklist";
    }

    private void addDashboardModel(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("users", adminService.getUserSummaries());
        model.addAttribute("recentReservations", adminService.getRecentReservations(80));
        model.addAttribute("activeBlacklists", adminService.getActiveBlacklists());
    }
}
