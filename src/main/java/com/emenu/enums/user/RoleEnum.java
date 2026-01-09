package com.emenu.enums.user;

import lombok.Getter;

@Getter
public enum RoleEnum {
    // Platform Roles
    PLATFORM_OWNER("Platform Owner", "Full platform control", true, false, false),
    PLATFORM_ADMIN("Platform Admin", "Platform administration", true, false, false),
    PLATFORM_MANAGER("Platform Manager", "Platform operations", true, false, false),
    PLATFORM_SUPPORT("Platform Support", "Customer support", true, false, false);

    private final String displayName;
    private final String description;
    private final boolean platformRole;
    private final boolean businessRole;
    private final boolean customerRole;

    RoleEnum(String displayName, String description, boolean platformRole, boolean businessRole, boolean customerRole) {
        this.displayName = displayName;
        this.description = description;
        this.platformRole = platformRole;
        this.businessRole = businessRole;
        this.customerRole = customerRole;
    }

}