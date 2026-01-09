package com.emenu.enums.user;

import lombok.Getter;

@Getter
public enum UserType {
    PLATFORM_USER("Platform User");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

}