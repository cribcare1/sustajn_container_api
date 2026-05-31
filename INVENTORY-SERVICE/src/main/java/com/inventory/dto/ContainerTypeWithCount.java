package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.inventory.entity.ContainerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTypeWithCount {

    // 🟢 The magic annotation that flattens your existing data into the JSON!
    @JsonUnwrapped
    private ContainerType containerType;

    // 🟢 Your two new required fields
    private Integer totalContainerCount;
    private Integer availableContainerCount;
}