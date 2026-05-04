//package com.auth.config;
//
//import feign.codec.Encoder;
//import feign.form.spring.SpringFormEncoder;
//import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
//import org.springframework.cloud.openfeign.support.SpringEncoder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class FeignConfig {
//
//    @Bean
//    public Encoder feignEncoder() {
//        return new SpringFormEncoder(new SpringEncoder(() -> new HttpMessageConverters()));
//    }
//}
