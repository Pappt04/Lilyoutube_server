package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.auth.*;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.service.AuthService;
import com.group17.lilyoutube_server.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest req) {
        baseUrl="http://localhost:8888";
        User user= authService.register(req);
        String activationLink = baseUrl + "/api/auth/activate?token=" + user.getActivationToken();
        emailService.sendSimpleEmail(user.getEmail(),"Activate your account", activationLink);

        return "We sent you an email, check it out to activate your account";
    }

    @GetMapping("/activate")
    public String activate(@RequestParam String token) {
        return authService.activate(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        return new AuthResponse(token);
    }
}
