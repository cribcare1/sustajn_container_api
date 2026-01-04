package com.inventory.config;

import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignMultipartConfig {

    @Bean
    public Encoder feignEncoder(ObjectFactory<FeignHttpMessageConverters> converters) {
        return new SpringEncoder((ObjectProvider<FeignHttpMessageConverters>) converters);
    }
}

