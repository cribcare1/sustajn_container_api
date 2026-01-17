package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeasedReturnedCountWithTimeGraphResponse {

    private Integer leasedReturnedCount;
    private String time; // Format: "0-1", "1-2", ..., "23-24"
}
