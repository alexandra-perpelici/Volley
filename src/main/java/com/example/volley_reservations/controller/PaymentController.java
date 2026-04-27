package com.example.volley_reservations.controller;

import com.example.volley_reservations.model.Payment;
import com.example.volley_reservations.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments/{token}")
    public String showPaymentTicket(@PathVariable String token,
                                    HttpServletRequest request,
                                    Model model) {
        Payment payment = paymentService.findByToken(token);
        addPaymentModel(token, request, model, payment);
        return "payment-ticket";
    }

    @GetMapping("/registry/payments/{token}")
    public String showRegistryConfirmation(@PathVariable String token,
                                           HttpServletRequest request,
                                           Model model) {
        Payment payment = paymentService.findByToken(token);
        addPaymentModel(token, request, model, payment);
        return "registry-payment";
    }

    @PostMapping("/registry/payments/{token}/confirm")
    public String confirmRegistryPayment(@PathVariable String token,
                                         HttpServletRequest request,
                                         Model model) {
        Payment payment = paymentService.confirmPayment(token);
        addPaymentModel(token, request, model, payment);
        model.addAttribute("confirmedNow", true);
        return "registry-payment";
    }

    private void addPaymentModel(String token, HttpServletRequest request, Model model, Payment payment) {
        String registryUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/registry/payments/" + token)
                .replaceQuery(null)
                .build()
                .toUriString();

        model.addAttribute("payment", payment);
        model.addAttribute("registryUrl", registryUrl);
    }
}
