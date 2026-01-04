package com.auth.request;

import com.auth.validation.CreateGroup;
import com.auth.validation.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {

    @NotNull(message = "Address id can't be null", groups = UpdateGroup.class)
    private Long addressId;
    @NotNull(message = "User id can't be null", groups = CreateGroup.class)
    private Long userId;
    private String addressType;
    @NotBlank(message = "Flat/Door/House details can't be null or empty", groups = CreateGroup.class)
    private String flatDoorHouseDetails;
    @NotBlank(message = "Street/City/Block details can't be null or empty", groups = CreateGroup.class)
    private String areaStreetCityBlockDetails;
    @NotBlank(message = "PO Box/Postal code details can't be null or empty", groups = CreateGroup.class)
    private String poBoxOrPostalCode;

}
