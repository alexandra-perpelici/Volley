package com.example.volley_reservations.service;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.model.Reservation;
import com.example.volley_reservations.repository.ReservationRepository;
import com.example.volley_reservations.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Optional;

import static java.time.LocalDate.*;

@Service
public class ReservationService {
    private final UserRepository userRepository;
    Reservation reservation;
   private final ReservationRepository reservationRepository;

   // inject repository so i can call findReservation method
    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    public boolean isReservedField1(String key)
    {
        String[] parts = key.split(" ", 2);
        String datePart = parts[0];
        String hourPart = parts[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate localDate = parse(datePart, formatter);

        Optional<Reservation> reservation = reservationRepository.findReservation(localDate, hourPart,1);
        return reservation.isPresent();
    }

    public boolean isReservedField2(String key)
    {
        String[] parts = key.split(" ",2);
        String datePart = parts[0];
        String hourPart = parts[1];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate localDate = parse(datePart, formatter);


        Optional<Reservation> reservation = reservationRepository.findReservation(localDate, hourPart,2);
        return reservation.isPresent();
    }

    public void deleteReservation(int reservationId) {
        reservationRepository.deleteById(reservationId);
    }


    public void deleteOldReservations(){
        LocalDate today = LocalDate.now();
        reservationRepository.deleteAllBeforeToday(today);

    }

    public String createNewReservation(ReservationRequest request)
    {
        if (reservationRepository.existsReservation(request.getUser_id(), request.getReservation_date(), request.getReservation_time())) {
            return "Reservation already exists for this time slot!";
        }

           reservation = new Reservation();
        reservation.setReservation_date(request.getReservation_date());
        reservation.setReservation_time(request.getReservation_time());
        reservation.setUser(userRepository.findUserById(request.getUser_id()));
        reservation.setField_number(request.getField_number());

        reservationRepository.save(reservation);
        return "Reservation Created";





    }

}
