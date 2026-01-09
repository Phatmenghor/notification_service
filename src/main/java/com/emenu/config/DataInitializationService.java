package com.emenu.config;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.enums.user.UserType;
import com.emenu.features.auth.models.Role;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.RoleRepository;
import com.emenu.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Object initLock = new Object();

    @Value("${app.init.create-admin:true}")
    private boolean createDefaultAdmin;

    @Value("${app.init.admin-email:phatmenghor19@gmail.com}")
    private String defaultAdminEmail;

    @Value("${app.init.admin-password:88889999}")
    private String defaultAdminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        if (initialized.get()) {
            log.info("Data initialization already completed. Skipping...");
            return;
        }

        synchronized (initLock) {
            if (initialized.get()) {
                log.info("Data initialization already completed (double-check). Skipping...");
                return;
            }

            try {
                log.info("🚀 Starting Cambodia E-Menu Platform data initialization...");

                // Initialize in strict order
                int rolesCreated = ensureRolesExist();
                log.info("✅ Roles initialization completed - {} roles processed", rolesCreated);


                if (createDefaultAdmin) {
                    int usersCreated = initializeDefaultUsers();
                    log.info("✅ Default users initialization completed - {} users processed", usersCreated);
                }

                // Mark as initialized only after all steps complete
                initialized.set(true);
                log.info("🎉 Cambodia E-Menu Platform data initialization completed successfully!");

            } catch (Exception e) {
                log.error("❌ Error during data initialization: {}", e.getMessage(), e);
                // Don't set initialized flag on failure so it can be retried
                throw new RuntimeException("Data initialization failed", e);
            }
        }
    }

    private int ensureRolesExist() {
        try {
            log.info("🔄 Ensuring system roles exist...");

            // Get all existing roles from database in one query
            List<RoleEnum> existingRoles = roleRepository.findAll()
                    .stream()
                    .map(Role::getName)
                    .toList();

            // Get all enum values
            List<RoleEnum> allEnumRoles = Arrays.asList(RoleEnum.values());

            // Find missing roles
            List<RoleEnum> missingRoles = allEnumRoles.stream()
                    .filter(roleEnum -> !existingRoles.contains(roleEnum))
                    .toList();

            if (missingRoles.isEmpty()) {
                log.info("✅ All {} system roles already exist: {}", allEnumRoles.size(), existingRoles);
                return existingRoles.size();
            } else {
                log.info("🔧 Found {} missing roles out of {}: {}", missingRoles.size(), allEnumRoles.size(), missingRoles);
                log.info("📋 Existing roles in database: {}", existingRoles);
                
                int createdCount = createMissingRoles(missingRoles);
                log.info("✅ Successfully created {} missing roles", createdCount);
                
                return existingRoles.size() + createdCount;
            }

        } catch (Exception e) {
            log.error("❌ Error during roles verification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to ensure roles exist", e);
        }
    }

    private int createMissingRoles(List<RoleEnum> missingRoles) {
        int createdCount = 0;
        
        for (RoleEnum roleEnum : missingRoles) {
            try {
                if (roleRepository.existsByName(roleEnum)) {
                    log.debug("Role {} already exists (created by another process)", roleEnum.name());
                    continue;
                }

                Role role = new Role(roleEnum);
                role = roleRepository.save(role);
                createdCount++;
                
                log.info("✅ Successfully created role: {} with ID: {}", roleEnum.name(), role.getId());
                
            } catch (Exception e) {
                // ✅ ENHANCED: Handle potential constraint violations gracefully
                if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                    log.warn("⚠️ Role {} likely already exists (constraint violation), continuing...", roleEnum.name());
                    continue;
                } else {
                    log.error("❌ Error creating role {}: {}", roleEnum.name(), e.getMessage());
                    throw new RuntimeException("Failed to create role: " + roleEnum.name(), e);
                }
            }
        }
        
        return createdCount;
    }

    private int initializeDefaultUsers() {
        try {
            log.info("🔄 Initializing default users...");
            
            int usersCreated = 0;
            usersCreated += createPlatformOwner();
            
            return usersCreated;
            
        } catch (Exception e) {
            log.error("❌ Error initializing default users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize default users", e);
        }
    }

    private int createPlatformOwner() {
        try {
            String adminUserIdentifier = defaultAdminEmail;

            if (!userRepository.existsByUserIdentifierAndIsDeletedFalse(adminUserIdentifier)) {
                User admin = new User();
                admin.setUserIdentifier(adminUserIdentifier);
                admin.setEmail(defaultAdminEmail);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setFirstName("Platform");
                admin.setLastName("Administrator");
                admin.setUserType(UserType.PLATFORM_USER);
                admin.setPosition("Platform Owner");
                admin.setAccountStatus(AccountStatus.ACTIVE);

                Role platformOwnerRole = roleRepository.findByName(RoleEnum.PLATFORM_OWNER)
                        .orElseThrow(() -> new RuntimeException("Platform owner role not found"));
                admin.setRoles(List.of(platformOwnerRole));

                admin = userRepository.save(admin);
                log.info("✅ Created platform owner: {} with ID: {}", adminUserIdentifier, admin.getId());
                return 1;
            } else {
                log.info("ℹ️ Platform owner already exists: {}", adminUserIdentifier);
                return 0;
            }
        } catch (Exception e) {
            log.error("❌ Error creating platform owner: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create platform owner", e);
        }
    }
}