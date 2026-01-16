package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeasedReturnedResponse {

    private String monthYear;   // November-2025
    private String date;        // 22.11.2025
    private Long count;         // leased count

//    public LeasedReturnedResponse(String monthYear, String date, Long count) {
//        this.monthYear = monthYear;
//        this.date = date;
//        this.count = count;
//    }
}
