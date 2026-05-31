package com.auth.controller;

import com.auth.model.User;
import com.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final UserService userService;

    @GetMapping("/getUserByEmail/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email){
        User user=userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
