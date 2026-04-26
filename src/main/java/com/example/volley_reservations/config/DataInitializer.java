//package com.example.volley_reservations.config;
//
//import com.example.volley_reservations.model.Reservation;
//import com.example.volley_reservations.model.User;
//import com.example.volley_reservations.repository.ReservationRepository;
//import com.example.volley_reservations.repository.UserRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.sql.Date;
//import java.time.LocalDate;
//
//@Configuration
//public class DataInitializer {
//
//    @Bean
//    CommandLineRunner initDatabase(UserRepository userRepository, ReservationRepository reservationRepository) {
//        return args -> {
//           User user1 = userRepository.findUserById(1);
//           User user2 = userRepository.findUserById(2);
//
//            // Today + Tomorrow
//            LocalDate today = LocalDate.now();
//            LocalDate dayAfterTomorrow = today.plusDays(2);
//
//            // Add reservations for Field 1
//            Reservation r1 = new Reservation();
//            r1.setReservation_date(today);
//            r1.setReservation_time("17:00 - 18:30");
//            r1.setField_number(2);
//            r1.setUser(user1);
//            reservationRepository.save(r1);
//
//            Reservation r2 = new Reservation();
//            r2.setReservation_date(dayAfterTomorrow);
//            r2.setReservation_time("08:00 - 09:30");
//            r2.setField_number(1);
//            r2.setUser(user1);
//            reservationRepository.save(r2);
//
//
//
//
//
//            System.out.println("Sample data added to database!");
//        };
//    }
//}
//
