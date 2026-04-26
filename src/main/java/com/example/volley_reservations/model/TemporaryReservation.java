package com.example.volley_reservations.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class TemporaryReservation {

    private LocalDate date;
    private String time;
    private int fieldNumber;
    private LocalDateTime expireAt;

    public TemporaryReservation(LocalDate date, String time, int fieldNumber, LocalDateTime expireAt) {
        this.date = date;
        this.time = time;
        this.fieldNumber = fieldNumber;
        this.expireAt = expireAt;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public int getFieldNumber() { return fieldNumber; }
    public LocalDateTime getExpireAt() { return expireAt; }

    // Equals and hashCode based on date, time, and fieldNumber
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporaryReservation)) return false;
        TemporaryReservation that = (TemporaryReservation) o;
        return fieldNumber == that.fieldNumber &&
                Objects.equals(date, that.date) &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, fieldNumber);
    }
}
