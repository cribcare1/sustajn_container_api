package com.auth.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {

    private Long addressId;
    private Long userId;
    private String addressType;
    private String flatDoorHouseDetails;
    private String areaStreetCityBlockDetails;
    private String poBoxOrPostalCode;

}
