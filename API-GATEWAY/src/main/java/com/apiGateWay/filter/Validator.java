package com.apiGateWay.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class Validator {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

//    public static final List<String> publicEndpoints = List.of(
//            "/register-user",
//            "/login",
//            "/register-restaurant",
//            "/registerCostumer",
//            "/validate-token/*",   // use wildcard instead of {token}
//            "/change-password"
//    );
public static final List<String> publicEndpoints = List.of(
        "/auth/register-user",
        "/auth/login",
        "/auth/register-restaurant",
        "/auth/registerCostumer",
        "/auth/validate-token/**", // match any token path
        "/auth/change-password/**",
        "/auth/images/**",
        "/inventory/subscription-plans/getPlans/**"

);


    public Predicate<ServerHttpRequest> predicate = request -> {
        String path = request.getURI().getPath();
        // Returns true if JWT validation IS REQUIRED
        return publicEndpoints.stream()
                .noneMatch(uri -> antPathMatcher.match(uri, path));
    };
}