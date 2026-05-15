package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailedSoldMonthResponse {
    private String monthYear;
    private Integer monthWiseTotalSoldContainers;


    private List<DetailedSoldProductResponse> dateWiseSoldContainers;
}