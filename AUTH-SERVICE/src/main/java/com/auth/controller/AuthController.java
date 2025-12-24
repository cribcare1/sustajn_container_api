package com.auth.controller;

import com.auth.feignClient.service.NotificationFeignClientService;
import com.auth.model.User;
import com.auth.model.UserDto;
import com.auth.repository.UserRepository;
import com.auth.request.ChangePasswordRequest;
import com.auth.request.LoginRequest;
import com.auth.request.RestaurantRegistrationRequest;
import com.auth.request.UpdateProfileRequest;
import com.auth.response.LoginResponse;
import com.auth.response.ProfileResponse;
import com.auth.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final NotificationFeignClientService notificationFeignClientService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
//    @PostMapping("/register-user")
//    public UserDto registerUser(@RequestBody User user){
//        UserDto userDto = userService.saveUser(user);
//        return userDto;
//    }
    @PostMapping("/register-user")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        try {
            UserDto userDto = userService.saveUser(user);

            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "User registered successfully",
                            "data", userDto
                    )
            );

        } catch (RuntimeException ex) {

            return ResponseEntity.ok(
                    Map.of(
                            "status", "ERROR",
                            "message", ex.getMessage()
                    )
            );

        } catch (Exception ex) {

            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "success",
                            "message", "Something went wrong"+ ex.getMessage()
                    )
            );
        }
    }
    

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestBody LoginRequest loginRequest) {

        try {
            // 1️⃣ Check if user exists
            Optional<User> userOpt = userRepository.findByUserName(loginRequest.getUserName());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of(
                                "status", "ERROR",
                                "message", "Username not found"
                        )
                );
            }

            User user = userOpt.get();

            // 2️⃣ Check password manually
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of(
                                "status", "ERROR",
                                "message", "Invalid password"
                        )
                );
            }

            // 3️⃣ Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
                            loginRequest.getPassword()
                    )
            );

            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of(
                                "status", "ERROR",
                                "message", "Authentication failed"
                        )
                );
            }

            // 4️⃣ Generate token
            LoginResponse response = userService.generateToken(loginRequest.getUserName());

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Login successful",
                            "data", response
                    )
            );

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    Map.of(
                            "status", "ERROR",
                            "message", "Something went wrong"
                    )
            );
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
    @GetMapping("/{restaurantId}/getProfile")
    public ProfileResponse getRestaurantProfileById(
            @PathVariable Long restaurantId
    ) {
        return userService.getRestaurantProfileById(restaurantId);
    }
    @PutMapping("/{restaurantId}/profile")
    public ProfileResponse updateRestaurantProfileById(
            @PathVariable Long restaurantId,
            @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateRestaurantProfileById(restaurantId, request);
    }

}
