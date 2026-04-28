package com.example.volley_reservations.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_blacklist_entries")
public class UserBlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklist_id")
    private Integer blacklistId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "created_by_admin_id")
    private User createdByAdmin;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public Integer getBlacklistId() {
        return blacklistId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public User getCreatedByAdmin() {
        return createdByAdmin;
    }

    public void setCreatedByAdmin(User createdByAdmin) {
        this.createdByAdmin = createdByAdmin;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return revokedAt == null && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public String getStatusLabel() {
        if (revokedAt != null) {
            return "Ridicata";
        }
        if (LocalDateTime.now().isAfter(endsAt)) {
            return "Expirata";
        }
        return "Activa";
    }
}
