package com.chill_guys.vitae_backend.auth;

import com.chill_guys.vitae_backend.auth.model.entity.OauthAccount;
import com.chill_guys.vitae_backend.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, UUID> {
    Optional<OauthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OauthAccount> findByUserAndProvider(User user, String provider);
}
