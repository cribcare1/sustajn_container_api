package com.notification.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import com.notification.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final Map<String, String> tokenMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> expiryTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${notification.token.expiry-ms:120000}")
    private long tokenExpiryMs;

    public String generateToken(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException("email is required", HttpStatus.BAD_REQUEST);
        }

        int numericToken  = (int)(Math.random() * 900000) + 100000;
        String token = String.valueOf(numericToken);
        // cancel previous expiry task if any
        ScheduledFuture<?> previous = expiryTasks.remove(email);
        if (previous != null) previous.cancel(false);

        tokenMap.put(email, token);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            tokenMap.remove(email);
            expiryTasks.remove(email);
        }, tokenExpiryMs, TimeUnit.MILLISECONDS);

        expiryTasks.put(email, future);
        return token;
    }

    public void verifyToken(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank()) {
            throw new ApiException("email and token are required", HttpStatus.BAD_REQUEST);
        }
        String existing = tokenMap.get(email);
        if (existing == null || !existing.equals(token)) {
            throw new ApiException("invalid or expired token", HttpStatus.BAD_REQUEST);
        }
        // consume token
        tokenMap.remove(email);
        ScheduledFuture<?> f = expiryTasks.remove(email);
        if (f != null) f.cancel(false);
    }

    public void removeToken(String email) {
        tokenMap.remove(email);
        ScheduledFuture<?> f = expiryTasks.remove(email);
        if (f != null) f.cancel(false);
    }

    public Instant getExpiryInstant(String email) {
        // best-effort: not tracking exact expiry time, return now + expiry
        if (!tokenMap.containsKey(email)) return null;
        return Instant.now().plusMillis(tokenExpiryMs);
    }
}
