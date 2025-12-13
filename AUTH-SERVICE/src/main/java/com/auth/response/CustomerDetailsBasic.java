package com.auth.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerDetailsBasic {
    private Long id;
    private String email;
    private String mobile;
    private String fullName;
    private String profileImage;
    private Integer borrowedCount;
    private Integer returnedCount;
    private Integer pendingCount;
}

