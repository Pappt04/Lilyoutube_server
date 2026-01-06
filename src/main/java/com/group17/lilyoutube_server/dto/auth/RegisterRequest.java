package com.group17.lilyoutube_server.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank @Email
    public String email;

    @NotBlank @Size(min = 3, max = 30)
    public String username;

    @NotBlank @Size(min = 8, max = 64)
    public String password;

    @NotBlank
    public String confirmPassword;

    @NotBlank
    public String firstName;

    @NotBlank
    public String lastName;

    @NotBlank
    public String address;
}
