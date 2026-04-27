package com.example.volley_reservations.service;

import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.UserBlacklistEntry;
import com.example.volley_reservations.repository.UserBlacklistRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UserBlacklistService {

    private static final int DEFAULT_BLACKLIST_DAYS = 14;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final UserBlacklistRepository blacklistRepository;

    public UserBlacklistService(UserBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserBlacklistEntry> findActiveForUser(User user) {
        if (user == null || user.getUser_id() == null) {
            return Optional.empty();
        }
        return findActiveForUserId(user.getUser_id());
    }

    @Transactional(readOnly = true)
    public Optional<UserBlacklistEntry> findActiveForUserId(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return blacklistRepository.findActiveForUser(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(User user) {
        return findActiveForUser(user).isPresent();
    }

    @Transactional(readOnly = true)
    public void validateUserCanReserve(User user) {
        Optional<UserBlacklistEntry> activeBlacklist = findActiveForUser(user);
        validateActiveBlacklist(activeBlacklist);
    }

    @Transactional(readOnly = true)
    public void validateUserCanReserve(Integer userId) {
        Optional<UserBlacklistEntry> activeBlacklist = findActiveForUserId(userId);
        validateActiveBlacklist(activeBlacklist);
    }

    private void validateActiveBlacklist(Optional<UserBlacklistEntry> activeBlacklist) {
        if (activeBlacklist.isPresent()) {
            UserBlacklistEntry entry = activeBlacklist.get();
            throw new IllegalStateException("This account is blacklisted from reservations until "
                    + entry.getEndsAt().format(DISPLAY_FORMAT) + ".");
        }
    }

    @Transactional
    public UserBlacklistEntry createTwoWeekBlacklist(User user, User admin, Reservation reservation, String reason) {
        Optional<UserBlacklistEntry> activeBlacklist = findActiveForUser(user);
        if (activeBlacklist.isPresent()) {
            return activeBlacklist.get();
        }

        LocalDateTime now = LocalDateTime.now();
        UserBlacklistEntry entry = new UserBlacklistEntry();
        entry.setUser(user);
        entry.setCreatedByAdmin(admin);
        entry.setReservation(reservation);
        entry.setStartsAt(now);
        entry.setEndsAt(now.plusDays(DEFAULT_BLACKLIST_DAYS));
        entry.setCreatedAt(now);
        entry.setReason(normalizeReason(reason));

        return blacklistRepository.save(entry);
    }

    @Transactional
    public void revokeBlacklist(Integer blacklistId) {
        UserBlacklistEntry entry = blacklistRepository.findById(blacklistId)
                .orElseThrow(() -> new EntityNotFoundException("Blacklist entry not found"));
        if (entry.getRevokedAt() == null) {
            entry.setRevokedAt(LocalDateTime.now());
        }
    }

    @Transactional(readOnly = true)
    public List<UserBlacklistEntry> findActiveEntries() {
        return blacklistRepository.findActiveEntries(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<UserBlacklistEntry> findHistoryForUser(Integer userId) {
        return blacklistRepository.findHistoryForUser(userId);
    }

    @Transactional(readOnly = true)
    public long countActiveEntries() {
        return blacklistRepository.countActiveEntries(LocalDateTime.now());
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "No-show";
        }
        return reason.trim();
    }
}
