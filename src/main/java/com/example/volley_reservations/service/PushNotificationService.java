package com.example.volley_reservations.service;

import com.example.volley_reservations.event.ReservationNotificationEvent;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.WebPushSubscription;
import com.example.volley_reservations.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationService.class);
    private static final int TTL_SECONDS = 60 * 60 * 24;

    private final WebPushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;

    private PushService pushService;

    public PushNotificationService(WebPushSubscriptionRepository subscriptionRepository,
                                   ObjectMapper objectMapper,
                                   @Value("${push.vapid.public-key:}") String vapidPublicKey,
                                   @Value("${push.vapid.private-key:}") String vapidPrivateKey,
                                   @Value("${push.vapid.subject:mailto:admin@volley.local}") String vapidSubject) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.vapidSubject = vapidSubject;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(vapidPublicKey) && StringUtils.hasText(vapidPrivateKey);
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    public void sendTestToUser(User user) {
        if (!isConfigured()) {
            throw new IllegalStateException("Notificarile push nu sunt configurate.");
        }

        String payload = toJson("Alertele Riviera sunt active", "Vei primi notificari cand apare o rezervare noua.", "/admin", "volley-test");
        subscriptionRepository.findByActiveTrueAndUser(user)
                .forEach(subscription -> sendToSubscription(subscription, payload));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendReservationNotification(ReservationNotificationEvent event) {
        if (!isConfigured()) {
            return;
        }

        List<WebPushSubscription> adminSubscriptions = subscriptionRepository.findActiveByUserRole("ADMIN");
        if (adminSubscriptions.isEmpty()) {
            return;
        }

        String payload = toJson("Rezervare noua", buildReservationBody(event), "/admin", "reservation-" + System.currentTimeMillis());
        adminSubscriptions.forEach(subscription -> sendToSubscription(subscription, payload));
    }

    private String buildReservationBody(ReservationNotificationEvent event) {
        if (event.slots().size() == 1) {
            ReservationNotificationEvent.ReservationNotificationSlot slot = event.slots().getFirst();
            return event.username() + " a rezervat Riviera " + slot.fieldNumber() + " pe "
                    + slot.date().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + ", " + slot.time() + ".";
        }

        return event.username() + " a rezervat " + event.slots().size() + " intervale pentru " + event.amountLei() + " RON.";
    }

    private String toJson(String title, String body, String url, String tag) {
        Map<String, String> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("url", url);
        payload.put("tag", tag);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nu s-a putut crea notificarea push.", exception);
        }
    }

    private void sendToSubscription(WebPushSubscription savedSubscription, String payload) {
        try {
            ensureBouncyCastleProvider();
            Notification notification = new Notification(
                    savedSubscription.getEndpoint(),
                    savedSubscription.getP256dh(),
                    savedSubscription.getAuth(),
                    payload.getBytes(StandardCharsets.UTF_8),
                    TTL_SECONDS
            );
            PushSendResult result = sendModern(notification);
            if (result.statusCode() == 403 || result.statusCode() == 404 || result.statusCode() == 410) {
                disableSubscription(savedSubscription);
            }
            if (result.statusCode() >= 400) {
                LOGGER.warn(
                        "Push notification failed for subscription {} with status {} {}. Body: {}",
                        savedSubscription.getSubscriptionId(),
                        result.statusCode(),
                        result.reason(),
                        result.body()
                );
            }
        } catch (Exception exception) {
            LOGGER.warn("Push notification failed for subscription {}", savedSubscription.getSubscriptionId(), exception);
        }
    }

    private PushService getPushService() throws GeneralSecurityException {
        if (pushService == null) {
            ensureBouncyCastleProvider();
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        }
        return pushService;
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private PushSendResult sendModern(Notification notification) throws IOException, GeneralSecurityException, org.jose4j.lang.JoseException {
        HttpPost httpPost = getPushService().preparePost(notification, Encoding.AES128GCM);
        httpPost.removeHeaders("Crypto-Key");

        try (CloseableHttpClient httpClient = HttpClients.createSystem();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String body = "";
            if (response.getEntity() != null) {
                body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
            return new PushSendResult(
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(),
                    body
            );
        }
    }

    private void disableSubscription(WebPushSubscription subscription) {
        subscription.setActive(false);
        subscription.setDisabledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    private record PushSendResult(int statusCode, String reason, String body) {
    }
}
