package com.inventory.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.inventory.util.DateTimeUtil;

@Entity
@Table(name = "admin_restaurant_inventory_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRestaurantInventoryDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "container_type_id", nullable = false)
    private Integer containerTypeId;

    @Column(name = "container_count", nullable = false)
    private Integer containerCount;

    // BORROW = Admin gives containers
    // RETURN = Restaurant returns containers
    @Column(name = "action_type", nullable = false, length = 10)
    private String actionType;  // BORROW / RETURN

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;

    @PrePersist
    public void onCreate() {
        this.createdOn = DateTimeUtil.nowDubai();
        this.updatedOn = DateTimeUtil.nowDubai();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = DateTimeUtil.nowDubai();
    }
}
