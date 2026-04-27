package com.example.volley_reservations.repository;

import com.example.volley_reservations.model.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @EntityGraph(attributePaths = {"reservations", "user"})
    Optional<Payment> findByConfirmationToken(String confirmationToken);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(payment.amountLei), 0) FROM Payment payment WHERE payment.status = 'CONFIRMED'")
    long sumConfirmedAmountLei();
}
