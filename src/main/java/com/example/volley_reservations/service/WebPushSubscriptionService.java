package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.PushSubscriptionRequest;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.WebPushSubscription;
import com.example.volley_reservations.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository subscriptionRepository;

    public WebPushSubscriptionService(WebPushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public WebPushSubscription saveSubscription(User user, PushSubscriptionRequest request, String userAgent) {
        validate(request);

        WebPushSubscription subscription = subscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(WebPushSubscription::new);

        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getKeys().getP256dh());
        subscription.setAuth(request.getKeys().getAuth());
        subscription.setUser(user);
        subscription.setUserAgent(truncate(userAgent));
        subscription.setActive(true);
        subscription.setDisabledAt(null);
        subscription.setLastSeenAt(LocalDateTime.now());

        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(LocalDateTime.now());
        }

        return subscriptionRepository.save(subscription);
    }

    private void validate(PushSubscriptionRequest request) {
        if (request == null || !StringUtils.hasText(request.getEndpoint()) || request.getKeys() == null) {
            throw new IllegalArgumentException("Push subscription is incomplete.");
        }

        if (!StringUtils.hasText(request.getKeys().getP256dh()) || !StringUtils.hasText(request.getKeys().getAuth())) {
            throw new IllegalArgumentException("Push subscription keys are incomplete.");
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
