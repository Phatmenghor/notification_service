package com.emenu.features.auth.dto.request;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.enums.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserCreateRequest {

    @NotBlank(message = "User identifier is required")
    private String userIdentifier;

    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 100)
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private String position;
    private String address;
    private String notes;
    
    @NotNull(message = "User type is required")
    private UserType userType;
    
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @NotNull(message = "At least one role is required")
    private List<RoleEnum> roles;
}