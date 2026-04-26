package com.example.volley_reservations.repository;

import com.example.volley_reservations.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Query("SELECT r FROM Reservation r WHERE r.reservation_date = :date AND r.reservation_time = :time AND r.field_number = :field")
    Optional<Reservation> findReservation(@Param("date") LocalDate date,
                                          @Param("time") String time,
                                          @Param("field") int field_number);



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


}

