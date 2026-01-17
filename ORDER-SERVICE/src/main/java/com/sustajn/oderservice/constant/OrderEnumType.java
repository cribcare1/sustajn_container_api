package com.sustajn.oderservice.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderEnumType {

    LEASED,
    RETURNED,
    UNKNOWN;

    @JsonCreator
    public static OrderEnumType fromValue(String value) {
        try {
            return OrderEnumType.valueOf(value);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
