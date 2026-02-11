package com.group17.lilyoutube_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.lilyoutube_server.dto.auth.LoginRequest;
import com.group17.lilyoutube_server.dto.auth.RegisterRequest;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.group17.lilyoutube_server.repository.PostRepository postRepository;

    @Autowired
    private com.group17.lilyoutube_server.repository.AuthTokenRepository authTokenRepository;

    @Autowired
    private com.group17.lilyoutube_server.repository.CommentRepository commentRepository;

    @Autowired
    private com.group17.lilyoutube_server.repository.LikeRepository likeRepository;

    @BeforeEach
    public void setup() {
        authTokenRepository.deleteAll();
        likeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testLoginWithDisabledUser() throws Exception {
        // Create a disabled user
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEnabled(false); // Disabled
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.email = "test@example.com";
        loginRequest.password = "password";

        // Expectation: Should return 400/403. Currently returns 403.
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden()) // Changed to 403
                .andDo(result -> System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString()));
    }
}
