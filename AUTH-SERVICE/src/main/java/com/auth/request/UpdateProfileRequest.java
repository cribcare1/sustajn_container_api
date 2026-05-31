package com.auth.request;

import com.auth.validation.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotNull(message = "Please provide user id", groups = UpdateGroup.class)
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String secondaryNumber;
    private String dateOfBirth;
}
