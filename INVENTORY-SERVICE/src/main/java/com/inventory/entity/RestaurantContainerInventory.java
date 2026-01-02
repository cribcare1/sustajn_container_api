package com.inventory.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_container_inventory",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"restaurantId", "containerTypeId"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RestaurantContainerInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;

    private Integer containerTypeId;

    private Integer currentQuantity;   // containers currently with restaurant

    private LocalDateTime lastUpdated;
}

