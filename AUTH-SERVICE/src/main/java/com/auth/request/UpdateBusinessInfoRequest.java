package com.auth.request;

import lombok.Data;

@Data
public class UpdateBusinessInfoRequest {
    private String businessType;
    private String website;
}