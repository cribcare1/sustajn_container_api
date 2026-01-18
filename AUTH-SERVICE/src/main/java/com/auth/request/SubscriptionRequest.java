package com.auth.request;

import com.auth.validation.CreateGroup;
import com.auth.validation.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {
    @NotNull(message = "User ID cannot be null", groups = {CreateGroup.class, UpdateGroup.class})
    private Long userId;
    @NotNull(message = "Subscription Plan ID cannot be null", groups = {CreateGroup.class, UpdateGroup.class})
    private Integer subscriptionPlanId;
}
