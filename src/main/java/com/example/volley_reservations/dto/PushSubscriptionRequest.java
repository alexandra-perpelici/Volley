package com.example.volley_reservations.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PushSubscriptionRequest {

    private String endpoint;
    private PushSubscriptionKeys keys;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public PushSubscriptionKeys getKeys() {
        return keys;
    }

    public void setKeys(PushSubscriptionKeys keys) {
        this.keys = keys;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PushSubscriptionKeys {
        private String p256dh;
        private String auth;

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
