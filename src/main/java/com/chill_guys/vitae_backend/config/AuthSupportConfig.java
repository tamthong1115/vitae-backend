package com.chill_guys.vitae_backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AuthSupportConfig {

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId
    ) {
        var transport = new NetHttpTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();
        return new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(List.of(googleClientId))
                .build();
    }


}
