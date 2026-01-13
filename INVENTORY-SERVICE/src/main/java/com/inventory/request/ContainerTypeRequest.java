package com.inventory.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerTypeRequest {

    private Integer id; // Null for new create

//    @NotBlank(message = "Container name is required")
    private String name;

    private String description;

    @Positive(message = "Capacity should be positive")
    private Integer capacityMl;

    private String material;
    private String colour;

    @DecimalMin(value = "0.0", message = "Length must be positive")
    private BigDecimal lengthCm;

    @DecimalMin(value = "0.0", message = "Width must be positive")
    private BigDecimal widthCm;

    @DecimalMin(value = "0.0", message = "Height must be positive")
    private BigDecimal heightCm;

    private Integer weightGrams;
    private Boolean foodSafe;
    private Boolean dishwasherSafe;
    private Boolean microwaveSafe;
    private Integer maxTemperature;
    private Integer minTemperature;
    private Integer lifespanCycle;

}

