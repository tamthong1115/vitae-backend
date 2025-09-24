package com.chill_guys.vitae_backend.user.model.dto;

import com.chill_guys.vitae_backend.user.model.entity.Role;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDTO(
        String id,
        String accountName,
        String email,
        String fullName,
        boolean verified,
        String status,
        List<Role.RoleName> roles,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {
}
