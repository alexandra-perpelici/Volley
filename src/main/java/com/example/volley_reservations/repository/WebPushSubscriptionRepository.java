package com.example.volley_reservations.repository;

import com.example.volley_reservations.model.User;
import com.example.volley_reservations.model.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Integer> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    List<WebPushSubscription> findByActiveTrueAndUser(User user);

    @Query("SELECT subscription FROM WebPushSubscription subscription JOIN FETCH subscription.user WHERE subscription.active = true AND subscription.user.role = :role")
    List<WebPushSubscription> findActiveByUserRole(@Param("role") String role);
}
