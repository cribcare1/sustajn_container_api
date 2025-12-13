package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.inventory.util.DateTimeUtil;

@Entity
@Table(
    name = "container_types",
    indexes = {
        @Index(name = "idx_container_name", columnList = "container_type_name"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerType extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "container_type_name", length = 100, nullable = false)
    private String name;

    @Column(name = "container_description", length = 200)
    private String description;

    @Column(name = "capacity_ml")
    private Integer capacityMl;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "container_material", length = 100)
    private String material;

    @Column(name = "container_colour", length = 100)
    private String colour;

    @Column(name = "dimension_length_cm", precision = 6, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "dimension_width_cm", precision = 6, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "dimension_height_cm", precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "is_food_safe")
    private Boolean foodSafe;

    @Column(name = "is_dishwash_safe")
    private Boolean dishwasherSafe;

    @Column(name = "is_microwave_safe")
    private Boolean microwaveSafe;

    @Column(name = "max_temperature")
    private Integer maxTemperature;

    @Column(name = "min_temperature")
    private Integer minTemperature;

    @Column(name = "lifespan_cycle")
    private Integer lifespanCycle;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cost_per_unit", precision = 8, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "status", length = 15)
    private String status; // active, inactive, discontinued

}
            