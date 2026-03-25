package com.etisalat.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 200)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked = false;


    private String createdByIp;
    private String replacedByTokenHash;
    @Column(unique = true)
    private String tokenId;

    @Column(nullable = false, length = 200)
    private String tokenSecretHash;

}
