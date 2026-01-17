package com.auth.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserActionRequest {

    private Long userId;
    private String approveStatus;
    private String remark;
}
