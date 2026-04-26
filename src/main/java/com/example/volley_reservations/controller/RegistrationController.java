package com.example.volley_reservations.controller;

import com.example.volley_reservations.dto.RegistrationRequest;
import com.example.volley_reservations.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "register";
    }
    @PostMapping("/register")
    public String register(@ModelAttribute("registrationRequest") RegistrationRequest request,
                           RedirectAttributes redirectAttributes) {

        String resultMessage = userService.registerUser(request);

        if(resultMessage.equals("User registered successfully")) {
            redirectAttributes.addFlashAttribute("successMessage", resultMessage);
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", resultMessage);
            redirectAttributes.addFlashAttribute("registrationRequest", request); // keep entered data
            return "redirect:/register"; // failure → back to registration form
        }
    }
    }

