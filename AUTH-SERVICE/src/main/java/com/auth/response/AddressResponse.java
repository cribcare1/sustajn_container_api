package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {

    private Long id;
    private String addressType;
    private String flatDoorHouseDetails;
    private String areaStreetCityBlockDetails;
    private String poBoxOrPostalCode;
}
