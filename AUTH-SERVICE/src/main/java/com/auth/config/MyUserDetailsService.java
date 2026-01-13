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
            case suspended -> throw new ResourceNotFoundException("User account is suspended. Contact support.");
            case inactive -> throw new ResourceNotFoundException("User account is inactive. Please activate first.");
            case active -> {} // allowed to proceed
            default -> throw new ResourceNotFoundException("Invalid account status!");
        }

        return new MyUserDetails(user);
    }

}
