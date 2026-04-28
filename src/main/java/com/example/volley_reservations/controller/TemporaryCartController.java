package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.security.CustomUserDetails;
import com.example.volley_reservations.service.BookingPricing;
import com.example.volley_reservations.service.PaymentService;
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
    private final PaymentService paymentService;
    private final UserBlacklistService blacklistService;

    public TemporaryCartController(TemporaryReservationService tempResService,
                                   UserRepository userRepository,
                                   PaymentService paymentService,
                                   UserBlacklistService blacklistService) {
        this.tempResService = tempResService;
        this.userRepository = userRepository;
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
            redirectAttributes.addFlashAttribute("message", "Teren invalid.");
            return "redirect:/home";
        }

        Integer userId = currentUserId(authentication);
        try {
            blacklistService.validateUserCanReserve(userId);
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            return "redirect:/field/" + fieldNumber;
        }

        boolean success = tempResService.addTemporaryReservation(userId, reservationDate, reservationTime, fieldNumber);

        if (!success) {
            redirectAttributes.addFlashAttribute("message", "Ora este blocata temporar de alt utilizator.");
            return "redirect:/field/" + fieldNumber;
        }

        redirectAttributes.addFlashAttribute("message", "Ora a fost adaugata in cos.");
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

        Integer userId = currentUserId(authentication);

        List<TemporaryReservation> cart = tempResService.getUserReservations(userId);
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", BookingPricing.totalPriceRon(cart));
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", BookingPricing.totalPriceRon(cart));
        model.addAttribute("slotPrice", BookingPricing.defaultSlotPriceRon());
        model.addAttribute("pricePerHour", BookingPricing.PRICE_PER_HOUR_RON);
        model.addAttribute("activeBlacklist", blacklistService.findActiveForUserId(userId).orElse(null));
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

        tempResService.removeReservation(currentUserId(authentication),
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

    // Confirm slots and record that payment will be cash at the field.
    @PostMapping("/confirm")
    public String confirmCart(@RequestParam(required = false) Integer fieldNumber,
                              Authentication authentication,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = currentUserId(authentication);
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<TemporaryReservation> cart = tempResService.getUserReservations(userId);

        try {
            blacklistService.validateUserCanReserve(user);
            paymentService.createCashAtFieldReservation(user, cart);
            tempResService.clearUserReservations(user.getUser_id());
            redirectAttributes.addFlashAttribute("message", "Rezervarea a fost confirmata. Plata se face cash la teren.");
            return "redirect:/profile";
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

    private Integer currentUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new IllegalStateException("Authenticated user details unavailable");
    }


}
