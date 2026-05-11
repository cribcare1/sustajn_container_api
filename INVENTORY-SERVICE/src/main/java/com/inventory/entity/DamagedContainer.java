package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "damaged_container")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DamagedContainer extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "container_type_id", nullable = false)
    private Integer containerTypeId;
    @Column(name = "remark", length = 500)
    private String remark;
    @Column(name = "restaurant_id")
    private Long restaurantId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "damaged_by_restaurant")
    private Boolean damagedByRestaurant;
    @Column(name = "damaged_by_user")
    private Boolean damagedByUser;
    private Integer damagedCount;
}
