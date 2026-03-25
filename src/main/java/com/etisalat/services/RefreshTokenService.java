package com.etisalat.services;

import com.etisalat.config.JwtProperties;
import com.etisalat.models.RefreshToken;
import com.etisalat.models.User;
import com.etisalat.repos.RefreshTokenRepository;
import com.etisalat.utils.HashUtils;
import com.etisalat.utils.TokenUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {


    private final RefreshTokenRepository repo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long refreshValidityMs;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repo, BCryptPasswordEncoder passwordEncoder,
                               org.springframework.core.env.Environment env,
                               JwtProperties properties) {
        this.repo = repo;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.refreshValidityMs = Long.parseLong(env.getProperty("app.jwt.refresh-token-validity-ms", "2592000000"));
    }

    /**
     * Returns cookieValue = tokenId + ":" + rawSecret
     */
    public String createRefreshToken(User user, String createdByIp) {
        String tokenId = UUID.randomUUID().toString();
        String rawSecret = TokenUtils.generateRandomToken(64);

        String secretHash = HashUtils.sha256(rawSecret);

        RefreshToken token = new RefreshToken();
        token.setTokenId(tokenId);
        token.setTokenSecretHash(secretHash);
        token.setUser(user);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusMillis(refreshValidityMs));
        token.setCreatedByIp(createdByIp);
        token.setRevoked(false);
        token.setTokenHash(rawSecret);
        repo.save(token);

        return tokenId + ":" + rawSecret;
    }

    public Optional<RefreshToken> verifyAndGet(String cookieValue) {
        if (cookieValue == null || !cookieValue.contains(":")) return Optional.empty();
        String[] parts = cookieValue.split(":", 2);
        String tokenId = parts[0];
        String rawSecret = parts[1];

        Optional<RefreshToken> opt = repo.findByTokenId(tokenId);
        if (opt.isEmpty()) return Optional.empty();
        RefreshToken token = opt.get();

        if (token.isRevoked()) return Optional.empty();
        if (token.getExpiresAt().isBefore(Instant.now())) return Optional.empty();

        String hashedRawSecret = HashUtils.sha256(rawSecret);
        if (!hashedRawSecret.equals(token.getTokenSecretHash())) return Optional.empty();

        return Optional.of(token);
    }

    public void revoke(RefreshToken token, String replacedByTokenId) {
        token.setRevoked(true);
        token.setReplacedByTokenHash(replacedByTokenId);
        repo.save(token);
    }

    public void revokeAllForUser(User user) {
        repo.deleteAllByUser(user);
    }
}
