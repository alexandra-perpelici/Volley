package com.example.volley_reservations.repository;

import com.example.volley_reservations.model.UserBlacklistEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlacklistRepository extends JpaRepository<UserBlacklistEntry, Integer> {

    @EntityGraph(attributePaths = {"user", "reservation", "createdByAdmin"})
    @Query("SELECT entry FROM UserBlacklistEntry entry " +
            "WHERE entry.user.user_id = :userId " +
            "AND entry.revokedAt IS NULL " +
            "AND entry.startsAt <= :now " +
            "AND entry.endsAt > :now " +
            "ORDER BY entry.endsAt DESC")
    Optional<UserBlacklistEntry> findActiveForUser(@Param("userId") Integer userId,
                                                   @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"user", "reservation", "createdByAdmin"})
    @Query("SELECT entry FROM UserBlacklistEntry entry " +
            "WHERE entry.revokedAt IS NULL " +
            "AND entry.startsAt <= :now " +
            "AND entry.endsAt > :now " +
            "ORDER BY entry.endsAt ASC")
    List<UserBlacklistEntry> findActiveEntries(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(entry) FROM UserBlacklistEntry entry " +
            "WHERE entry.revokedAt IS NULL " +
            "AND entry.startsAt <= :now " +
            "AND entry.endsAt > :now")
    long countActiveEntries(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"user", "reservation", "createdByAdmin"})
    @Query("SELECT entry FROM UserBlacklistEntry entry " +
            "WHERE entry.user.user_id = :userId " +
            "ORDER BY entry.createdAt DESC")
    List<UserBlacklistEntry> findHistoryForUser(@Param("userId") Integer userId);
}
