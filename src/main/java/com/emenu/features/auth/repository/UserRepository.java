package com.emenu.features.auth.repository;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.enums.user.UserType;
import com.emenu.features.auth.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserIdentifierAndIsDeletedFalse(String userIdentifier);

    boolean existsByUserIdentifierAndIsDeletedFalse(String userIdentifier);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.roles r " +
            "WHERE u.isDeleted = false " +
            "AND (:userTypes IS NULL OR u.userType IN :userTypes) " +
            "AND (:accountStatuses IS NULL OR u.accountStatus IN :accountStatuses) " +
            "AND (:roles IS NULL OR r.name IN :roles) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "    LOWER(u.userIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(
            @Param("userTypes") List<UserType> userTypes,
            @Param("accountStatuses") List<AccountStatus> accountStatuses,
            @Param("roles") List<RoleEnum> roles,
            @Param("search") String search,
            Pageable pageable
    );
}