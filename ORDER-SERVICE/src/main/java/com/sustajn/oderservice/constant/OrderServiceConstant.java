package com.sustajn.oderservice.constant;

import java.util.Map;

public class OrderServiceConstant {
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String MESSAGE = "message";
    public  static final String STATUS="status";
    public static final String LEASED = "leased";
    public static final String RETURNED = "returned";
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String ACTION = "action";
    public static final Map<String, String> ACTION_BORROW = Map.of("action", "borrow");
    public static final Map<String, String> ACTION_RETURN = Map.of("action", "return");
}