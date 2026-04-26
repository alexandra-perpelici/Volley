package com.example.volley_reservations.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ReservationRequest {

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    LocalDate reservation_date;
    String reservation_time;
    Integer user_id;
    int field_number;

    public void setReservation_date(LocalDate reservation_date) {
        this.reservation_date = reservation_date;
    }
    public LocalDate getReservation_date() {
        return reservation_date;
    }

    public void setReservation_time(String reservation_time) {
        this.reservation_time = reservation_time;
    }
   public String getReservation_time() {
        return reservation_time;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }
    public Integer getUser_id() {
        return user_id;
    }
    public void setField_number(int field_number) {
        this.field_number = field_number;
    }
   public int getField_number() {
        return field_number;
    }


}
