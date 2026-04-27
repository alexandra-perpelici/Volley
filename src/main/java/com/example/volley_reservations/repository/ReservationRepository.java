package com.example.volley_reservations.repository;

import com.example.volley_reservations.model.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Query("SELECT r FROM Reservation r WHERE r.reservation_date = :date AND r.reservation_time = :time AND r.field_number = :field")
    Optional<Reservation> findReservation(@Param("date") LocalDate date,
                                          @Param("time") String time,
                                          @Param("field") int field_number);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.field_number = :fieldNumber " +
            "AND r.reservation_date BETWEEN :startDate AND :endDate")
    List<Reservation> findReservationsForFieldBetweenDates(@Param("fieldNumber") int fieldNumber,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"user", "payment"})
    @Query("SELECT r FROM Reservation r ORDER BY r.reservation_date DESC, r.reservation_time ASC")
    List<Reservation> findAdminReservations(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "payment"})
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.user.user_id = :userId " +
            "ORDER BY r.reservation_date DESC, r.reservation_time ASC")
    List<Reservation> findAdminReservationsForUser(@Param("userId") Integer userId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.user_id = :userId")
    long countReservationsForUser(@Param("userId") Integer userId);

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.user.user_id = :userId " +
            "AND r.reservation_date >= :today " +
            "AND r.status <> 'CANCELLED'")
    long countUpcomingReservationsForUser(@Param("userId") Integer userId,
                                          @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.user.user_id = :userId " +
            "AND r.status = 'NO_SHOW'")
    long countNoShowsForUser(@Param("userId") Integer userId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.reservation_date = :date")
    long countReservationsOnDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.reservation_date BETWEEN :startDate AND :endDate")
    long countReservationsBetweenDates(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.status = :status " +
            "AND r.reservation_date BETWEEN :startDate AND :endDate")
    long countReservationsByStatusBetweenDates(@Param("status") String status,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM Reservation r WHERE r.reservation_date < :today")
    void deleteAllBeforeToday(LocalDate today);


    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Reservation r " +
            "WHERE r.user.user_id = :userId " +
            "AND r.reservation_date = :reservationDate " +
            "AND r.reservation_time = :reservationTime")
    boolean existsReservation(@Param("userId") Integer userId,
                              @Param("reservationDate") LocalDate reservationDate,
                              @Param("reservationTime") String reservationTime);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Reservation r " +
            "WHERE r.reservation_date = :reservationDate " +
            "AND r.reservation_time = :reservationTime " +
            "AND r.field_number = :fieldNumber")
    boolean existsSlot(@Param("reservationDate") LocalDate reservationDate,
                       @Param("reservationTime") String reservationTime,
                       @Param("fieldNumber") int fieldNumber);


}

