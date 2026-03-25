package com.etisalat.repos;

import com.etisalat.models.RefreshToken;
import com.etisalat.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteAllByUser(User user);
    Optional<RefreshToken> findByTokenId(String tokenId);

}
