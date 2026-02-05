package com.group17.lilyoutube_server.config;

import com.group17.lilyoutube_server.service.DbUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public DaoAuthenticationProvider authenticationProvider(
                        DbUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                return provider;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setExposedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        TokenAuthFilter tokenAuthFilter,
                        LoginRateLimitFilter loginRateLimitFilter,
                        DaoAuthenticationProvider authProvider,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authProvider)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api/internal/**",
                                                                "/actuator/health",
                                                                "/api/stream/**",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/**",
                                                                "/api/posts", "/api/posts/**", "/api/media/**",
                                                                "/api/comments", "/api/comments/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setHeader("Access-Control-Allow-Origin", "*");
                                                        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
                                                        response.setHeader("Access-Control-Allow-Headers", "*");
                                                        response.setHeader("Access-Control-Expose-Headers", "*");
                                                        response.setHeader("Access-Control-Allow-Credentials", "true");
                                                        response.setHeader("Access-Control-Max-Age", "3600");
                                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                                                }));

                http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

                http.addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
