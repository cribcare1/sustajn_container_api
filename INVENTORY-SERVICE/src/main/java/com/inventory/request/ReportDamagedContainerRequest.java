package com.inventory.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportDamagedContainerRequest {

    @NotNull(message = "Container type id cannot be null", groups = {com.inventory.validation.CreateGroup.class})
    private Integer containerTypeId;
    private String remark;
    private Long restaurantId;
    private Long userId;
    private Boolean isDamagedByRestaurant;
    private Boolean isDamagedByUser;
    private Integer damagedCount;
}
