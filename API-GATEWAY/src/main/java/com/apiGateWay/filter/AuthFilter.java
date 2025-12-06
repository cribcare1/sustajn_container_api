package com.apiGateWay.filter;

import com.apiGateWay.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Validator validator;

    public AuthFilter(){
        super(Config.class);
    }


//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            if(validator.predicate.test(exchange.getRequest())) {
//                if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
//                    throw new BadRequestException("Authorization header is missing", HttpStatus.UNAUTHORIZED);
//                }
//
//                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
//                String token = null;
//                if(null != authHeader && authHeader.startsWith("Bearer ")){
//                    token = authHeader.substring(7);
//                }
//                try{
//                    jwtUtil.validateToke(token);
//                }catch (Exception e){
//                    throw new BadRequestException("Invalid token", HttpStatus.UNAUTHORIZED);
//                }
//            }
//            return chain.filter(exchange);
//        };
//    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (validator.predicate.test(exchange.getRequest())) {

                if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                    return Mono.error(new BadRequestException("Authorization header missing", HttpStatus.UNAUTHORIZED));
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                String token = null;

                if(authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }

                try{
                    jwtUtil.validateToke(token);
                } catch (Exception e){
                    return Mono.error(new BadRequestException("Invalid token: " + e.getMessage(), HttpStatus.UNAUTHORIZED));
                }
            }

            return chain.filter(exchange);
        };
    }


    public static class Config{

    }
}