package com.auth.controller;

import com.auth.model.User;
import com.auth.repository.UserRepository;
import com.auth.response.ProfileResponse;
import com.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @GetMapping
    public ProfileResponse getProfile(HttpServletRequest request) {


        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing");
        }


        String token = authHeader.substring(7);


        String username = jwtUtil.extractUsername(token);




        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));


        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAddress(),
                user.getPhoneNumber(),
                user.getProfilePictureUrl()
        );
    }
}
