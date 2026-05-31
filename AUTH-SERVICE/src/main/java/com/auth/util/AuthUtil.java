package com.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthUtil {

    public static <T> T convertToJson(String jsonString, Class<T> cls) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, cls);
        } catch (IOException err) {
            err.printStackTrace();
            return null;
        }
    }
}
