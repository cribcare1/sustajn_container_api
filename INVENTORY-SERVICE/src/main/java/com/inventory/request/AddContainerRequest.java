package com.inventory.request;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class AddContainerRequest {

    @NotBlank(message = "Container name is required")
    @Size(max = 100, message = "Container name must be less than 100 characters")
    private String containerName;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Capacity in ml is required")
    @Min(value = 1, message = "Capacity must be at least 1 ml")
    private Integer capacityMl;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String description;
    private String material;
    private String colour;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private Integer weightGrams;
    private Boolean foodSafe;
    private Boolean dishwasherSafe;
    private Boolean microwaveSafe;
    private Integer maxTemperature;
    private Integer minTemperature;
    private Integer lifespanCycle;
    private Long userId; // User performing the action
}
