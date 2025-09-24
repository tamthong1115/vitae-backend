package com.chill_guys.vitae_backend.config;

import com.chill_guys.vitae_backend.user.UserRepository;
import com.chill_guys.vitae_backend.user.model.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String accountNameOrEmail){
        User u = userRepository.findByEmailOrAccountName(accountNameOrEmail)
                .orElseThrow(() -> new RuntimeException("User not found with account name or email: " + accountNameOrEmail));
        return new UserPrincipal(u);
    }
}
