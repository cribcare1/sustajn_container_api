package com.auth.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder(){
        return  new BCryptPasswordEncoder();
    }
//    @Bean
//     public SecurityFilterChain  filterChain(HttpSecurity http){
//        http.csrf(AbstractHttpConfigurer ::  disable)
//                .authorizeHttpRequests(req->{
//                    req.requestMatchers("/auth/register-user","/auth/login","/auth/change-password","/auth/register-restaurant","/auth/registerCostumer","/auth/**").permitAll();
//                    req.requestMatchers(
//                            "/restaurants/**"
//                    ).permitAll();
//                    req.requestMatchers("/profile/**").authenticated();
//                    req.anyRequest().authenticated();
//                }).userDetailsService(userDetailsService())
//                .httpBasic(Customizer.withDefaults());
//        return http.build();
//}

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> {
                    // ✅ Public auth endpoints
                    req.requestMatchers(
                            "/auth/register-user",
                            "/auth/login",
                            "/auth/change-password",
                            "/auth/register-restaurant",
                            "/auth/registerCostumer",
                            "/auth/**"
                    ).permitAll();

                    // ✅ Public restaurant endpoints
                    req.requestMatchers("/restaurants/**").permitAll();

                    // ✅ Stripe webhook — must be public (Stripe sends raw POST, no token)
                    req.requestMatchers("/api/payments/webhook/stripe").permitAll();

                    // 🔒 Authenticated endpoints
                    req.requestMatchers("/profile/**").authenticated();

                    // 🔒 Everything else requires auth
                    req.anyRequest().authenticated();
                })
                .userDetailsService(userDetailsService())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // ✅ JWT-friendly
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(){
      return new MyUserDetailsService();
    }


    }
