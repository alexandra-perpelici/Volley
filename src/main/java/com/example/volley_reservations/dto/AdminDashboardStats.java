package com.example.volley_reservations.dto;

public record AdminDashboardStats(
        long totalUsers,
        long activeBlacklists,
        long reservationsToday,
        long reservationsThisWeek,
        long noShowsThisMonth,
        long pendingPayments,
        long confirmedRevenueLei
) {
}
