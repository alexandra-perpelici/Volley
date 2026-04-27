package com.example.volley_reservations.unitTests;

import com.example.volley_reservations.dto.RegistrationRequest;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegisterUser_PasswordsDoNotMatch() {

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("alex");
        request.setPassword("pass123");
        request.setConfirmPassword("differentPass"); // Mismatch

        String result = userService.registerUser(request);

        assertEquals("Passwords do not match", result);
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void testRegisterUser_Success() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newbieee");
        request.setPassword("securePass12");
        request.setConfirmPassword("securePass12");

        when(userRepository.findByUsername("newbieee")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("securePass12")).thenReturn("hashed_password");
        String result = userService.registerUser(request);
        assertEquals("User registered successfully", result);
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("newbieee") &&
                        user.getPassword().equals("hashed_password") &&
                        user.isActive() &&
                        user.getRole().equals("USER")
        ));
    }
}
