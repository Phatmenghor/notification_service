package com.emenu.features.auth.dto.response;

import com.emenu.enums.user.UserType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private UUID userId;
    
    private String userIdentifier;
    private String email;
    private String fullName;
    private String profileImageUrl;
    private UserType userType;
    private List<String> roles;
}