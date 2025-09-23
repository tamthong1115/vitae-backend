package com.chill_guys.vitae_backend.user.model.mapper;

import com.chill_guys.vitae_backend.user.model.dto.UserDTO;
import com.chill_guys.vitae_backend.user.model.entity.Role;
import com.chill_guys.vitae_backend.user.model.entity.User;

public final class UserMapper {
    private UserMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static UserDTO toDTO(User u) {
        return new UserDTO(
                u.getId().toString(),
                u.getAccountName(),
                u.getEmail(),
                u.getFullName(),
                u.isVerified(),
                u.getStatus() != null ? u.getStatus().name() : null,
                u.getRoles().stream().map(Role::getName).toList(),
                u.getLastLoginAt(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
