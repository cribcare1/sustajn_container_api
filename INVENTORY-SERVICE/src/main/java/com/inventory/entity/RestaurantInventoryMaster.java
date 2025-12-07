package com.inventory.entity;

import com.inventory.Constant.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantInventoryMaster extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long restaurantId;
    private Integer containerTypeId;
    private Integer totalContainers;
    private Integer availableContainers;
    private String borrowedContainers;
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

}

