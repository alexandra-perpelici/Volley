package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.TemporaryReservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.ReservationService;
import com.example.volley_reservations.service.TemporaryReservationService;
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

    public TemporaryCartController(TemporaryReservationService tempResService,
                                   UserRepository userRepository,
                                   ReservationService reservationService) {
        this.tempResService = tempResService;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
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

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        boolean success = tempResService.addTemporaryReservation(user.getUser_id(), reservationDate, reservationTime, fieldNumber);

        if (!success) {
            redirectAttributes.addFlashAttribute("message", "This slot is temporarily locked by another user.");
            return "redirect:/field/" + fieldNumber;
        }

        redirectAttributes.addAttribute("fieldNumber", fieldNumber);
        // Redirect with fieldNumber as query parameter
        return "redirect:/cart/view";
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
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        tempResService.removeReservation(user.getUser_id(),
                new TemporaryReservation(reservationDate, reservationTime, fieldNumber, null));

        redirectAttributes.addAttribute("fieldNumber",fieldNumber);

        if (originField != null) {
            return "redirect:/cart/view";
        }

        String previousPage = (String) session.getAttribute("previousPage");
        if (previousPage != null && previousPage.startsWith("field/")) {
            return "redirect:/" + previousPage;
        }

        return "redirect:/cart/view";
    }

    // Confirm and persist reservations
    @PostMapping("/confirm")
    public String confirmCart(@RequestParam(required = false) Integer fieldNumber,
                              Authentication authentication,
                              HttpSession session) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        tempResService.confirmReservations(user.getUser_id(), reservationService, user);

        if (fieldNumber != null) {
            return "redirect:/field/" + fieldNumber;
        }

        String previousPage = (String) session.getAttribute("previousPage");
        if (previousPage != null) {
            return "redirect:/" + previousPage;
        }

        return "redirect:/home";
    }




}
