package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeasedReturnedContainerCountResponse {

    private Integer leasedContainerCount;
    private Integer returnedContainerCount;
}
