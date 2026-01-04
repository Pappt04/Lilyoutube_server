package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.auth.LoginRequest;
import com.group17.lilyoutube_server.dto.auth.RegisterRequest;
import com.group17.lilyoutube_server.model.AuthToken;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.repository.AuthTokenRepository;
import com.group17.lilyoutube_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthService(UserRepository userRepository,
                       AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public String register(RegisterRequest req) {
        if (!req.password.equals(req.confirmPassword)) {
            throw new IllegalArgumentException("Lozinke se ne poklapaju.");
        }
        if (userRepository.existsByEmail(req.email)) {
            throw new IllegalArgumentException("Email je već zauzet.");
        }
        if (userRepository.existsByUsername(req.username)) {
            throw new IllegalArgumentException("Korisničko ime je već zauzeto.");
        }

        User u = new User();
        u.setEmail(req.email);
        u.setUsername(req.username);
        u.setPassword(passwordEncoder.encode(req.password));
        u.setFirstName(req.firstName);
        u.setLastName(req.lastName);
        u.setAddress(req.address);
        u.setEnabled(false);

        String token = UUID.randomUUID().toString();
        u.setActivationToken(token);

        userRepository.save(u);

        // DEV varijanta: ispiši link u konzolu
        String activationLink = baseUrl + "/api/auth/activate?token=" + token;
        System.out.println("ACTIVATION LINK: " + activationLink);

        return "Registracija uspešna. Proveri konzolu za aktivacioni link.";
    }

    public String activate(String token) {
        User u = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Neispravan token."));

        u.setEnabled(true);
        u.setActivationToken(null);
        userRepository.save(u);

        return "Nalog je aktiviran. Sada možeš da se prijaviš.";
    }

    public String login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email, req.password)
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Pogrešan email ili lozinka.");
        }

        User u = userRepository.findByEmail(req.email)
                .orElseThrow(() -> new IllegalArgumentException("Pogrešan email ili lozinka."));

        if (!u.isEnabled()) {
            throw new IllegalStateException("Nalog nije aktiviran.");
        }

        AuthToken t = new AuthToken();
        t.setToken(UUID.randomUUID().toString());
        t.setUser(u);
        t.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        authTokenRepository.save(t);

        return t.getToken();
    }
}
