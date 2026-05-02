package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartnerInfoDto {
    private String name;
    private String address;
}