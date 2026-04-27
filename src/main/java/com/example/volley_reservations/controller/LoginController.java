package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.TemporaryReservationService;
import com.example.volley_reservations.service.UserBlacklistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final TemporaryReservationService temporaryReservationService;
    private final UserBlacklistService blacklistService;

    public LoginController(UserRepository userRepository,
                           TemporaryReservationService temporaryReservationService,
                           UserBlacklistService blacklistService) {
        this.userRepository = userRepository;
        this.temporaryReservationService = temporaryReservationService;
        this.blacklistService = blacklistService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/home")
    public String homePage(Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<TemporaryReservation> cart = temporaryReservationService.getUserReservations(user.getUser_id());

        model.addAttribute("cart", cart);
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", cart.size() * 20);
        model.addAttribute("activeBlacklist", blacklistService.findActiveForUser(user).orElse(null));
        return "home";
    }

}
