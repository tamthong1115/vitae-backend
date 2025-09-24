package com.chill_guys.vitae_backend.user;

import com.chill_guys.vitae_backend.user.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(Role.RoleName name);
}
