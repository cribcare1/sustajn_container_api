package com.auth.config;

import com.auth.exception.ResourceNotFoundException;
import com.auth.model.User;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static com.auth.enumDetails.AccountStatus.*;

@RequiredArgsConstructor

public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    private  UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userName));

        switch (user.getAccountStatus()) {

            case SUSPENDED ->
                    throw new ResourceNotFoundException(
                            "Your account has been suspended. Please contact support for assistance."
                    );

            case INACTIVE ->
                    throw new ResourceNotFoundException(
                            "Your account is inactive. Please complete the activation process."
                    );

            case PENDING ->
                    throw new ResourceNotFoundException(
                            "Your account is under review. Please wait for approval."
                    );

            case REJECTED ->
                    throw new ResourceNotFoundException(
                            "Your registration was rejected. Please update your details and reapply."
                    );

            case ACTIVE -> {
                // ✅ allowed to proceed
            }

            default ->
                    throw new ResourceNotFoundException(
                            "Your account status is invalid. Please contact support."
                    );
        }


        return new MyUserDetails(user);
    }

}
