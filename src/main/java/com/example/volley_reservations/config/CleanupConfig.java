//package com.example.volley_reservations.config;
//
//import com.example.volley_reservations.repository.ReservationRepository;
//import com.example.volley_reservations.repository.UserRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class CleanupConfig {
//
//    @Bean
//    CommandLineRunner cleanUp(UserRepository userRepository, ReservationRepository reservationRepository) {
//        return args -> {
//           userRepository.deleteById(4);
//           userRepository.deleteById(5);
//
//        };
//    }
//}
