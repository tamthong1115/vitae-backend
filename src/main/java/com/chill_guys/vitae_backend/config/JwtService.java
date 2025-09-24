package com.chill_guys.vitae_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;


@Service
public class JwtService {
    @Value("${app.jwt.issuer}")
    private String jwtIssuer;
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.access-token-ttl}")
    private Long accessTokenTtl;
    @Value("${app.jwt.refresh-token-ttl}")
    private Long refreshTokenTtl;
    @Value("${app.jwt.clock-skew-seconds}")
    private Long clockSkewSeconds;

    private SecretKey key;
    private JwtParser parser;
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);


    @PostConstruct
    public void init() {
        byte[] secretBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        var builder = Jwts.parser()
                .requireIssuer(jwtIssuer)
                .verifyWith(key);
        if (clockSkewSeconds != null && clockSkewSeconds > 0) {
            builder.clockSkewSeconds(clockSkewSeconds);
        }
        this.parser = builder.build();
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        var b = Jwts.builder()
                .issuer(jwtIssuer)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtl)));
        if (claims != null && !claims.isEmpty()) {
            b.claims(claims);
        }
        return b.signWith(key).compact();
    }

    /**
     * Parse and return claims (verifies signature, exp, nbf, and required issuer).
     */
    public Claims parse(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parse(token).getSubject();
    }

    public <T> T getClaim(String token, String name, Class<T> type) {
        Object v = parse(token).get(name);
        return v == null ? null : type.cast(v);
    }

    public static String generateNewBase64Secret() {
        var fresh = Jwts.SIG.HS256.key().build();
        return Encoders.BASE64.encode(fresh.getEncoded());
    }

}
