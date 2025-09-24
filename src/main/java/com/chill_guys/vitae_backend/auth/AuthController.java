package com.chill_guys.vitae_backend.auth;

import com.chill_guys.vitae_backend.config.JwtService;
import com.chill_guys.vitae_backend.user.UserService;
import com.chill_guys.vitae_backend.user.model.dto.*;
import com.chill_guys.vitae_backend.user.model.entity.Role;
import com.chill_guys.vitae_backend.user.model.entity.User;
import com.chill_guys.vitae_backend.user.model.mapper.UserMapper;
import com.chill_guys.vitae_backend.util.IpResolver;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final UserService users;
    private final JwtService jwt;
    private final GoogleIdTokenVerifier googleVerifier;

    @Value("${app.jwt.refresh-token-ttl}")
    private Long refreshTokenTtl;


    public AuthController(AuthenticationManager authManager, UserService users, JwtService jwt, GoogleIdTokenVerifier googleVerifier) {
        this.authManager = authManager;
        this.users = users;
        this.jwt = jwt;
        this.googleVerifier = googleVerifier;
    }

    @PostMapping("/register")
    public UserDTO register(
            @Valid @RequestBody RegisterRequest req
    ) {
        User u = users.create(req.email(), req.accountName(), req.password());
        return UserMapper.toDTO(u);
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest httpReq
    ) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));
            var u = users.loadByEmailOrAccountName(req.usernameOrEmail());
            if (u == null) {
                throw new RuntimeException("Invalid credentials");
            }
            users.touchLogin(u.getId());

            var claims = Map.of(
                    "roles", u.getRoles().stream().map(Role::getName).toList(),
                    "email", u.getEmail());
            String accessToken = jwt.generateAccessToken(u.getId().toString(), claims);
            InetAddress ip = IpResolver.resolveInet(httpReq);
            String refreshToken = users.createRefreshToken(u, httpReq.getHeader("User-Agent"), ip, req.deviceId(), refreshTokenTtl);
            return new TokenResponse(accessToken, refreshToken, u.getId().toString(), UserMapper.toDTO(u));
        } catch (Exception e) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    @PostMapping("/social/google")
    public TokenResponse google(@RequestBody GoogleIdTokenRequest req, HttpServletRequest httpReq) throws Exception {
        GoogleIdToken idToken = googleVerifier.verify(req.idToken());
        if (idToken == null) {
            throw new RuntimeException("Invalid ID token");
        }
        var payload = idToken.getPayload();
        String sub = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = (String) payload.get("name");

        var u = users.upsertIdentity("google", sub, email, emailVerified, name, null, null, null, null);
        users.touchLogin(u.getId());

        var claims = Map.of(
                "roles", u.getRoles().stream().map(r -> r.getName().name()).toList(),
                "email", u.getEmail()
        );
        String accessToken = jwt.generateAccessToken(u.getId().toString(), claims);
        InetAddress ip = IpResolver.resolveInet(httpReq);
        String refreshToken = users.createRefreshToken(u, httpReq.getHeader("User-Agent"), ip, req.deviceId(), refreshTokenTtl);
        return new TokenResponse(accessToken, refreshToken, u.getId().toString(), UserMapper.toDTO(u));

    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest req) {
        // resolve user from session id part of the opaque token (left of '.')
        int dot = req.refreshToken().lastIndexOf('.');
        if (dot <= 0) {
            throw new RuntimeException("Invalid refresh token");
        }
        UUID sessionId = UUID.fromString(req.refreshToken().substring(0, dot));

        var u = users.getUserBySessionId(sessionId);
        if (u == null) {
            throw new RuntimeException("Invalid refresh token");
        }
        var claims = Map.of("roles", u.getRoles().stream().map(Role::getName).toList(),
                "email", u.getEmail());

        String newOpaque = users.rotateRefreshToken(req.refreshToken(), refreshTokenTtl);
        String newAccess = jwt.generateAccessToken(u.getId().toString(), claims);
        return new RefreshResponse(newAccess, newOpaque);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest req) {
        users.revokeSession(UUID.fromString(req.sessionId()), "user logout");
    }

    @PostMapping("/logout-all")
    public void logoutAll(@Valid @RequestBody LogoutAllRequest req) {
        users.revokeAllSessionsForUser(UUID.fromString(req.userId()), "global logout");
    }
}
