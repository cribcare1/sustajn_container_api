package com.sustajn.oderservice.request;

import com.sustajn.oderservice.constant.OrderEnumType;
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
public class LeasedReturnedGraphInput {
    @NotNull(message = "restaurantId cannot be null")
    private Long restaurantId;
    @NotNull(message = "productId cannot be null")
    private Integer productId;
    @NotNull(message = "type cannot be blank")
    private OrderEnumType type;
    private String date;
}
