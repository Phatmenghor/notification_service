package com.emenu.security;

import com.emenu.enums.user.AccountStatus;
import com.emenu.exception.custom.*;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String userIdentifier = authentication.getName();
        return userRepository.findByUserIdentifierAndIsDeletedFalse(userIdentifier)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    public void validateAccountStatus(User user) {
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ValidationException("Account is suspended");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new ValidationException("Account is inactive");
        }
    }

}
