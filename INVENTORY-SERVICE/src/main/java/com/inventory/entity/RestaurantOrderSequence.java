package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_order_sequence")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantOrderSequence extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", unique = true, nullable = false)
    private Long restaurantId;

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence;
}
