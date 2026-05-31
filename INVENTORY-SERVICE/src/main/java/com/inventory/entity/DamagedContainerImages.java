package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "damaged_container_images")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DamagedContainerImages extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "damage_id", nullable = false)
    private Long damageId; // FK reference to DamagedContainer
    @Column(name = "damage_image_url", nullable = false)
    private String damageImageUrl;

}
