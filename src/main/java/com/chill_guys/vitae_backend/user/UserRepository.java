package com.chill_guys.vitae_backend.user;

import com.chill_guys.vitae_backend.user.model.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountName(String accountName);
    Optional<User> findByEmail(String email);

    default Optional<User> findByEmailOrAccountName(String input) {
        Optional<User> userOpt = findByAccountName(input);
        if (userOpt.isPresent()) {
            return userOpt;
        }
        return findByEmail(input);
    }

    boolean existsByAccountName(String accountName);
    boolean existsByEmail(String email);
}
