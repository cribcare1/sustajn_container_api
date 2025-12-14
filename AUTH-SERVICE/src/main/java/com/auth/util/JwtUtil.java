package com.auth.util;

import com.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

//    public static final String SECRET_KEY = "ASHHDFHSOIUEUBDIFBUIEWGFVSDVFIWWEE487536DGKFHGHDSGFHKSDGFUEFUEVCUKEUFUDVCVDHSVHSDVHF";

    public static final String SECRET_KEY =
            Base64.getEncoder().encodeToString(
                    "MySuperSecretKeyForJwtSecurity1234567890".getBytes()
            );
    public String generateToken(String username){
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", username);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))
                .claims(claims)
                .signWith(getKey())
                .compact();
    }

    public String generateToken(UserDetails userDetails){
        User user = (User) userDetails;

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getUserType().name());

        return Jwts.builder()
                .subject(user.getUserName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
                .claims(claims)
                .signWith(getKey())
                .compact();
    }


    private Key getKey() {
        byte[] bytes = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(bytes);
    }

    private Claims getClaims(String token){
        return Jwts.parser().verifyWith((SecretKey) getKey())
                .build().parseSignedClaims(token)
                .getPayload();
    }

    public Date extractExpiration(String token){
        return getClaims(token).getExpiration();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }
}

