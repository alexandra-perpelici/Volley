package com.example.volley_reservations.dto;

import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.UserBlacklistEntry;

import java.util.List;

public record AdminUserDetail(
        AdminUserSummary summary,
        List<Reservation> reservations,
        List<UserBlacklistEntry> blacklistHistory
) {
}
