package com.etisalat.services;

import com.etisalat.config.JwtProperties;
import com.etisalat.models.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public String generateAccessToken(User u) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAccessTokenValidityMs());

        List<String> roles = u.getRoles().stream().map(r -> r.getName()).toList();

        return Jwts.builder()
                .setSubject(u.getUsername())
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseClaims(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(properties.getSecret().getBytes())).build().parseClaimsJws(token);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) parseClaims(token).getBody().get("roles");
    }
}
