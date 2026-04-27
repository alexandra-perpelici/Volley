package com.example.volley_reservations.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_confirmation_token", columnNames = "confirmation_token")
})
public class Payment {

    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_CASH_AT_REGISTRY = "CASH_AT_REGISTRY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "confirmation_token", nullable = false)
    private String confirmationToken;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "amount_lei", nullable = false)
    private int amountLei;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "payment")
    private List<Reservation> reservations = new ArrayList<>();

    public Integer getPaymentId() {
        return paymentId;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAmountLei() {
        return amountLei;
    }

    public void setAmountLei(int amountLei) {
        this.amountLei = amountLei;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public String getPaymentMethodLabel() {
        if (METHOD_CASH_AT_REGISTRY.equals(paymentMethod)) {
            return "Cash at registry";
        }
        return "Card at registry";
    }

    public String getStatusLabel() {
        if (STATUS_CONFIRMED.equals(status)) {
            return "Payment confirmed";
        }
        return "Pending registry confirmation";
    }
}
