package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.PaymentService;
import com.example.volley_reservations.service.ReservationService;
import com.example.volley_reservations.service.TemporaryReservationService;
import com.example.volley_reservations.service.UserBlacklistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class TemporaryCartController {

    private final TemporaryReservationService tempResService;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final UserBlacklistService blacklistService;

    public TemporaryCartController(TemporaryReservationService tempResService,
                                   UserRepository userRepository,
                                   ReservationService reservationService,
                                   PaymentService paymentService,
                                   UserBlacklistService blacklistService) {
        this.tempResService = tempResService;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/back")
    public String goBack(HttpSession session,
                         @RequestParam(required = false) Integer fieldNumber,
                         @RequestParam(required = false) String source) {

        if ("home".equals(source)) {
            return "redirect:/home";
        } else if (fieldNumber != null) {
            return "redirect:/field/" + fieldNumber;
        }

        // fallback if nothing is provided
        String previousPage = (String) session.getAttribute("previousPage");
        return previousPage != null ? "redirect:/" + previousPage : "redirect:/home";
    }

    // Add a reservation to cart
    @PostMapping("/add")
    public String addToCart(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate reservationDate,
            @RequestParam String reservationTime,
            @RequestParam int fieldNumber,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!isSupportedCourt(fieldNumber)) {
            redirectAttributes.addFlashAttribute("message", "Invalid court selected");
            return "redirect:/home";
        }

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        try {
            blacklistService.validateUserCanReserve(user);
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            return "redirect:/field/" + fieldNumber;
        }

        boolean success = tempResService.addTemporaryReservation(user.getUser_id(), reservationDate, reservationTime, fieldNumber);

        if (!success) {
            redirectAttributes.addFlashAttribute("message", "This slot is temporarily locked by another user.");
            return "redirect:/field/" + fieldNumber;
        }

        redirectAttributes.addFlashAttribute("message", "Slot added to your cart.");
        return "redirect:/field/" + fieldNumber;
    }

    // Show the cart
    @GetMapping("/view")
    public String showCart(
            HttpSession session,
            @RequestParam(required = false) Integer fieldNumber,
            @RequestParam(required = false) String source,
            Model model,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        List<TemporaryReservation> cart = tempResService.getUserReservations(user.getUser_id());
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", cart.size() * 20);
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", cart.size() * 20);
        model.addAttribute("activeBlacklist", blacklistService.findActiveForUser(user).orElse(null));
        model.addAttribute("source", source);


        // Update session-based previousPage only as backup
        if (source != null) {
            session.setAttribute("previousPage", source);
        } else if (fieldNumber != null) {
            session.setAttribute("previousPage", "field/" + fieldNumber);
        }

        model.addAttribute("fieldNumber", fieldNumber);
        return "cart";
    }
    @PostMapping("/delete")
    public String deleteFromCart(@RequestParam LocalDate reservationDate,
                                 @RequestParam String reservationTime,
                                 @RequestParam int fieldNumber,
                                 @RequestParam(required = false) Integer originField,
                                 @RequestParam(required = false) String source,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        tempResService.removeReservation(user.getUser_id(),
                new TemporaryReservation(reservationDate, reservationTime, fieldNumber, null));

        if ("home".equals(source)) {
            redirectAttributes.addAttribute("source", "home");
            return "redirect:/cart/view";
        }

        if (originField != null) {
            redirectAttributes.addAttribute("fieldNumber", originField);
            return "redirect:/cart/view";
        }

        String previousPage = (String) session.getAttribute("previousPage");
        if (previousPage != null && previousPage.startsWith("field/")) {
            return "redirect:/" + previousPage;
        }

        return "redirect:/cart/view";
    }

    // Confirm slots and create a pending payment ticket
    @PostMapping("/confirm")
    public String confirmCart(@RequestParam(required = false) Integer fieldNumber,
                              @RequestParam(required = false) String paymentMethod,
                              Authentication authentication,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<TemporaryReservation> cart = tempResService.getUserReservations(user.getUser_id());

        try {
            blacklistService.validateUserCanReserve(user);
            Payment payment = paymentService.createPendingPayment(user, cart, paymentMethod);
            tempResService.clearUserReservations(user.getUser_id());
            return "redirect:/payments/" + payment.getConfirmationToken();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            if (fieldNumber != null) {
                redirectAttributes.addAttribute("fieldNumber", fieldNumber);
            } else if ("home".equals(session.getAttribute("previousPage"))) {
                redirectAttributes.addAttribute("source", "home");
            }
            return "redirect:/cart/view";
        }
    }


    private boolean isSupportedCourt(int fieldNumber) {
        return fieldNumber == 1 || fieldNumber == 2;
    }


}
