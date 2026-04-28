package com.example.volley_reservations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;



public class RegistrationRequest {

    @NotBlank(message = "Utilizatorul este obligatoriu")
    private String username;

    @NotBlank(message = "Parola este obligatorie")
    @Size(min = 6, message = "Parola trebuie sa aiba cel putin 6 caractere")
    private String password;

    @NotBlank(message = "Confirmarea parolei este obligatorie")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;

    }
    public String getConfirmPassword() {
        return confirmPassword;
    }
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

}
