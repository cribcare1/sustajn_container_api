package com.payment.configuration;

//import feign.codec.Encoder;
//import feign.form.spring.SpringFormEncoder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class FeignClientConfig {
//
//    @Bean
//    public Encoder feignFormEncoder() {
//        return new SpringFormEncoder();
//    }
//}

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.lang.reflect.Type;

@Configuration
public class FeignClientConfig {

    @Bean
    public Encoder feignFormEncoder(ObjectMapper objectMapper) {
        return new SpringFormEncoder(new Encoder() {
            @Override
            public void encode(Object object, Type bodyType, feign.RequestTemplate template) {
                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(object);
                    template.body(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    template.header("Content-Type", "application/json");
                } catch (Exception e) {
                    throw new EncodeException("Failed to encode request body: " + e.getMessage(), e);
                }
            }
        });
    }
}