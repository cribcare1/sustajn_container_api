package com.inventory.dto;

import com.inventory.entity.SoldContainers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SoldContainerMonthWiseResponse {

    private String monthYear; // e.g., "January-2026"
    private Integer monthWiseTotalSoldContainers;
    private List<SoldContainersDateWiseResponse> dateWiseSoldContainers;
}
