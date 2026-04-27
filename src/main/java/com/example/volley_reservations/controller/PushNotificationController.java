package com.example.volley_reservations.controller;

import com.example.volley_reservations.dto.PushSubscriptionRequest;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.PushNotificationService;
import com.example.volley_reservations.service.WebPushSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final WebPushSubscriptionService subscriptionService;
    private final UserRepository userRepository;

    public PushNotificationController(PushNotificationService pushNotificationService,
                                      WebPushSubscriptionService subscriptionService,
                                      UserRepository userRepository) {
        this.pushNotificationService = pushNotificationService;
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/public-key")
    public Map<String, Object> publicKey() {
        return Map.of(
                "enabled", pushNotificationService.isConfigured(),
                "publicKey", pushNotificationService.getVapidPublicKey()
        );
    }

    @PostMapping("/subscriptions")
    public Map<String, String> subscribe(@RequestBody PushSubscriptionRequest request,
                                         HttpServletRequest servletRequest,
                                         Authentication authentication) {
        User user = currentUser(authentication);
        subscriptionService.saveSubscription(user, request, servletRequest.getHeader("User-Agent"));
        return Map.of("status", "subscribed");
    }

    @PostMapping("/test")
    public Map<String, String> sendTest(Authentication authentication) {
        pushNotificationService.sendTestToUser(currentUser(authentication));
        return Map.of("status", "sent");
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
