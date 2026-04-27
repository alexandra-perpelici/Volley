package com.example.volley_reservations.model;

import jakarta.persistence.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Entity
@Table(name = "reservations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_reservation_slot",
                columnNames = {"reservation_date", "reservation_time", "field_number"}
        )
})
public class Reservation {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_ATTENDED = "ATTENDED";
    public static final String STATUS_NO_SHOW = "NO_SHOW";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private int reservation_id;

    @NotNull
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservation_date;

    @NotNull
    @Column(name = "reservation_time", nullable = false)
    private String reservation_time;

    @Min(1)
    @Max(2)
    @Column(name = "field_number", nullable = false)
    private int field_number;

    @Column(nullable = false)
    private String status = STATUS_RESERVED;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // creates user_id column in reservation table
    private User user;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

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
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public Payment getPayment() {
        return payment;
    }
    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getStatusLabel() {
        if (STATUS_ATTENDED.equals(status)) {
            return "Attended";
        }
        if (STATUS_NO_SHOW.equals(status)) {
            return "No-show";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "Cancelled";
        }
        return "Reserved";
    }

}
