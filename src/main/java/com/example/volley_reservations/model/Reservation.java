package com.example.volley_reservations.model;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    private int reservation_id;

    @NotNull
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate reservation_date;

    @NotNull
    private String reservation_time;
    private int field_number;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // creates user_id column in reservation table
    private User user;

    public int getReservation_id() {
        return reservation_id;
    }
    public void setReservation_id(int reservation_id) {
        this.reservation_id = reservation_id;
    }
    public LocalDate getReservation_date() {
        return reservation_date;
    }
    public void setReservation_date(LocalDate reservation_date) {
        this.reservation_date = reservation_date;
    }
    public String getReservation_time() {
        return reservation_time;
    }
    public void setReservation_time(String reservation_time) {
        this.reservation_time = reservation_time;
    }
    public int getField_number() {
        return field_number;
    }

    public void setField_number(int field_number) {
        this.field_number = field_number;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

}
