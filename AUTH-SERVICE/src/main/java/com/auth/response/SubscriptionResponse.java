package com.auth.response;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {

    private Integer planId;

    private String planName;

    private String planType;

    private String description;

    private String partnerType;

    private BigDecimal feeType;

    private BigDecimal depositType;

    private BigDecimal commissionPercentage;

    private Integer minContainers;

    private Integer maxContainers;

    private Integer totalContainers;

    private Boolean includesDelivery;

    private Boolean includesMarketing;

    private Boolean includesAnalytics;

    private String billingCycle;

    private String planStatus;

}
