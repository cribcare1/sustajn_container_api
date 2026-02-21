package com.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sold_container")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SoldContainers extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "container_id")
    private Integer containerId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "restaurant_id")
    private Long restaurantId;
    @Column(name = "sold_quantity")
    private Integer soldQuantity;
    @Column(name = "sold_price")
    private Integer soldPrice;
}
