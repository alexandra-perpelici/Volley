package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.security.CustomUserDetails;
import com.example.volley_reservations.service.BookingPricing;
import com.example.volley_reservations.service.TemporaryReservationService;
import com.example.volley_reservations.service.UserBlacklistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LoginController {

    private final TemporaryReservationService temporaryReservationService;
    private final UserBlacklistService blacklistService;

    public LoginController(TemporaryReservationService temporaryReservationService,
                           UserBlacklistService blacklistService) {
        this.temporaryReservationService = temporaryReservationService;
        this.blacklistService = blacklistService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/home")
    public String homePage(Model model, Authentication authentication) {
        Integer userId = currentUserId(authentication);
        List<TemporaryReservation> cart = temporaryReservationService.getUserReservations(userId);

        model.addAttribute("cart", cart);
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", BookingPricing.totalPriceRon(cart));
        model.addAttribute("slotPrice", BookingPricing.defaultSlotPriceRon());
        model.addAttribute("pricePerHour", BookingPricing.PRICE_PER_HOUR_RON);
        model.addAttribute("activeBlacklist", blacklistService.findActiveForUserId(userId).orElse(null));
        return "home";
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new IllegalStateException("Authenticated user details unavailable");
    }

}
