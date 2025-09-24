package com.chill_guys.vitae_backend.config;

import com.chill_guys.vitae_backend.user.UserService;
import com.chill_guys.vitae_backend.user.model.entity.Role;
import com.chill_guys.vitae_backend.util.IpResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService users;

    @Value("${app.cookies.domain}")
    private String cookieDomain;
    @Value("${app.frontend.callback}")
    private String frontendCallback;
    @Value("${app.jwt.refresh-token-ttl}")
    private Long refreshTokenTtl;

    public OAuth2LoginSuccessHandler(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.users = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest req,
            HttpServletResponse res,
            Authentication auth
    ) {
        String email = null, name = null, providerUserId = null;
        boolean emailVerified = false;

        Object principal = auth.getPrincipal();
        // Google (OIDC)
        if (principal instanceof OidcUser oidc) { // Google
            email = oidc.getEmail();
            name = oidc.getFullName();
            providerUserId = oidc.getSubject();
            emailVerified = true; // per Google OIDC claim
            var u = users.upsertIdentity("google", providerUserId, email, emailVerified, name, null, null, null, null);
            issueCookiesAndRedirect(req, res, u.getId().toString(), u.getEmail(),
                    u.getRoles().stream().map(r -> r.getName()).toList());
            return;
        }

        if (principal instanceof DefaultOAuth2User ou) { // GitHub
            providerUserId = String.valueOf(ou.getAttributes().get("id"));
            name = (String) ou.getAttributes().getOrDefault("name", ou.getName());
            email = (String) ou.getAttributes().get("email"); // may be null; fetch verified via /user/emails in your provisioning
            emailVerified = email != null;
            var u = users.upsertIdentity("github", providerUserId, email, emailVerified, name, null, null, null, null);
            issueCookiesAndRedirect(req, res, u.getId().toString(), u.getEmail(),
                    u.getRoles().stream().map(r -> r.getName()).toList());
            return;
        }

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

    private void issueCookiesAndRedirect(
            HttpServletRequest req,
            HttpServletResponse res,
            String userId,
            String email,
            java.util.List<Role.RoleName> roles

    ) {
        String access = jwtService.generateAccessToken(userId, Map.of("email", email, "roles", roles));
        var user = users.loadByEmailOrAccountName(email);
        InetAddress ip = IpResolver.resolveInet(req);
        String opaqueRefresh = users.createRefreshToken(user,
                req.getHeader("User-Agent"),
                ip,
                null,
                refreshTokenTtl
        );

        Cookie accessCookie = new Cookie("access_token", access);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        if (!cookieDomain.isBlank()) {
            accessCookie.setDomain(cookieDomain);
        }

        Cookie refreshCookie = new Cookie("refresh_token", opaqueRefresh);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        if (!cookieDomain.isBlank()) {
            refreshCookie.setDomain(cookieDomain);
        }

        res.addCookie(accessCookie);
        res.addCookie(refreshCookie);

        try {
            String url = frontendCallback + "?ok=1&u=" + URLEncoder.encode(userId, StandardCharsets.UTF_8);
            res.sendRedirect(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
