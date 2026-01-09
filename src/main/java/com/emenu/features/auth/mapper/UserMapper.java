package com.emenu.features.auth.mapper;

import com.emenu.enums.user.RoleEnum;
import com.emenu.features.auth.dto.request.UserCreateRequest;
import com.emenu.features.auth.dto.response.LoginResponse;
import com.emenu.features.auth.dto.response.UserResponse;
import com.emenu.features.auth.dto.update.UserUpdateRequest;
import com.emenu.features.auth.models.Role;
import com.emenu.features.auth.models.User;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UserMapper {

    @Autowired
    protected PaginationMapper paginationMapper;

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToEnums")
    public abstract UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "roles", source = "user.roles", qualifiedByName = "rolesToStrings")
    @Mapping(target = "accessToken", source = "token")
    @Mapping(target = "tokenType", constant = "Bearer")
    public abstract LoginResponse toLoginResponse(User user, String token);

    public abstract List<UserResponse> toResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    public abstract void updateEntity(UserUpdateRequest request, @MappingTarget User user);

    public User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setUserIdentifier(request.getUserIdentifier());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setPosition(request.getPosition());
        user.setAddress(request.getAddress());
        user.setNotes(request.getNotes());
        user.setUserType(request.getUserType());
        user.setAccountStatus(request.getAccountStatus());
        return user;
    }

    @Named("rolesToEnums")
    protected List<RoleEnum> rolesToEnums(List<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(Role::getName).collect(Collectors.toList());
    }

    @Named("rolesToStrings")
    protected List<String> rolesToStrings(List<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(role -> role.getName().name()).collect(Collectors.toList());
    }

    public PaginationResponse<UserResponse> toPaginationResponse(Page<User> page) {
        return paginationMapper.toPaginationResponse(page, this::toResponseList);
    }
}