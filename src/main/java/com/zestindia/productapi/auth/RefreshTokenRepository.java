package com.zestindia.productapi.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.user.id = :userId")
    void revokeAllForUser(@Param("userId") Integer userId);
}
