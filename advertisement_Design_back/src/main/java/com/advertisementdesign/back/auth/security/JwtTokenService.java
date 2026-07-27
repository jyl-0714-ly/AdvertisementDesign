package com.advertisementdesign.back.auth.security;

import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.identity.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String createToken(CurrentUser user) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder().subject(String.valueOf(user.getId())).claim("email", user.getEmail())
                .claim("nickname", user.getNickname()).claim("role", user.getRole().name())
                .issuedAt(now).expiration(expireAt).signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    public CurrentUser parseToken(String token) {
        var claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return CurrentUser.builder().id(Long.valueOf(claims.getSubject())).email(claims.get("email", String.class))
                .nickname(claims.get("nickname", String.class)).role(UserRole.valueOf(claims.get("role", String.class))).build();
    }
}
