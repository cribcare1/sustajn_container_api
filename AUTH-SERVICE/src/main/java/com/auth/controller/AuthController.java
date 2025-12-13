package com.auth.controller;

import com.auth.exception.BadRequestException;
import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.ChangePasswordRequest;
import com.auth.request.LoginRequest;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.response.LoginResponse;
import com.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register-user")
    public UserDto registerUser(@RequestBody User user){
        UserDto userDto = userService.saveUser(user);
        return userDto;
    }

    @PostMapping("/login")
    public LoginResponse generateToken(@RequestBody LoginRequest loginRequest){

        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword()));

            if(authentication.isAuthenticated()){
                return userService.generateToken(loginRequest.getUserName());
            }else{
                throw new BadRequestException("Invalid Credentials");
            }

        }catch (Exception e){
            throw new BadRequestException("Invalid Credentials");
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest passwordRequest){
        Map<String,Object> response = userService.changePassword(passwordRequest.getUserId(),passwordRequest.getNewPassword());
        return ResponseEntity.ok(response);
    }


    @PostMapping(
            value = "/register-restaurant",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> registerRestaurant(
            @RequestPart("data") String data,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        RestaurantRegistrationRequest request =
                mapper.readValue(data, RestaurantRegistrationRequest.class);

        return ResponseEntity.ok(
                userService.registerRestaurant(request, profileImage)
        );
    }


}
