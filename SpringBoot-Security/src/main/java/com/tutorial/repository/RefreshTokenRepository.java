package com.tutorial.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorial.entity.RefreshToken;
import com.tutorial.enums.RevocationReason;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// =========================================================
	// FIND BY HASH
	// =========================================================

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	// =========================================================
	// FIND USER TOKENS
	// =========================================================

	List<RefreshToken> findAllByUserId(Long userId);

	// =========================================================
	// FIND TOKEN FAMILY
	// =========================================================

	List<RefreshToken> findAllByFamilyId(String familyId);

	// =========================================================
	// PESSIMISTIC WRITE LOCK
	// =========================================================

	/*
	 * Used during refresh-token rotation.
	 *
	 * Prevents two simultaneous requests from rotating the same refresh token at
	 * the same time.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    SELECT r
			    FROM RefreshToken r
			    WHERE r.tokenHash = :tokenHash
			""")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	// =========================================================
	// REVOKE TOKEN FAMILY
	// =========================================================

	@Modifying
	@Query("""
			    UPDATE RefreshToken r
			    SET r.revoked = true,
			        r.revokedAt = :now,
			        r.revocationReason = :reason
			    WHERE r.familyId = :familyId
			      AND r.revoked = false
			""")
	int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now,
			@Param("reason") RevocationReason reason);

	// =========================================================
	// DELETE EXPIRED TOKENS
	// =========================================================

	@Modifying
	@Query("""
			    DELETE FROM RefreshToken r
			    WHERE r.expiresAt < :now
			""")
	int deleteExpiredTokens(@Param("now") LocalDateTime now);
}