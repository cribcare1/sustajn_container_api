package com.auth.controller;

import com.auth.exception.BadRequestException;
import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.request.ChangePasswordRequest;
import com.auth.request.LoginRequest;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.response.LoginResponse;
import com.auth.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final NotificationFeignClientService notificationFeignClientService;

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
        Map<String,Object> response = userService.changePassword(passwordRequest.getEmail(),passwordRequest.getNewPassword());
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


    @PostMapping(
            value = "/registerCostumer",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> registerUserWithBankDetails(
            @RequestPart("data") String request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        RestaurantRegistrationRequest regRequest =
                mapper.readValue(request, RestaurantRegistrationRequest.class);
        return ResponseEntity.ok(
                userService.registerUserWithBankDetails(regRequest, profileImage)
        );
    }

    @GetMapping("/activeRestaurants")
    public ResponseEntity<?> getActiveRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> response = userService.getActiveRestaurantsMap(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }



    @GetMapping("/activeCustomersDetails")
    public ResponseEntity<?> getActiveCustomersDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> response = userService.getActiveCustomersMap(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }


    @GetMapping("/images/{type}/{fileName}")
    public ResponseEntity<byte[]> fetchImage(
            @PathVariable String type,
            @PathVariable String fileName) {

        byte[] imageBytes = notificationFeignClientService.getContainerImage(type, fileName);

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.IMAGE_JPEG) // You can dynamically detect type if needed
                .body(imageBytes);
    }

}
