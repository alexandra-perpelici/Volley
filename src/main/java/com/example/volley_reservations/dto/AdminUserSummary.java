package com.example.volley_reservations.dto;

import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.UserBlacklistEntry;

public record AdminUserSummary(
        User user,
        long totalReservations,
        long upcomingReservations,
        long noShowCount,
        UserBlacklistEntry activeBlacklist
) {
    public boolean hasActiveBlacklist() {
        return activeBlacklist != null;
    }
}
