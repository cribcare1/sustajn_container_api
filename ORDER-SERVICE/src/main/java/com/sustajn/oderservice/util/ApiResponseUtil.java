package com.sustajn.oderservice.util;

import com.sustajn.oderservice.constant.OrderServiceConstant;

import java.util.HashMap;
import java.util.Map;

public class ApiResponseUtil {

    public static Map<String, Object> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put(OrderServiceConstant.STATUS, OrderServiceConstant.STATUS_SUCCESS);
        response.put(OrderServiceConstant.MESSAGE, message);
        return response;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put(OrderServiceConstant.STATUS, OrderServiceConstant.STATUS_ERROR);
        response.put(OrderServiceConstant.MESSAGE, message);
        return response;
    }
}

