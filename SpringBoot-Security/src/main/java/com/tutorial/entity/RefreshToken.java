package com.tutorial.entity;

import java.time.LocalDateTime;

import com.tutorial.enums.RevocationReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens", indexes = {

		@Index(name = "idx_refresh_token_hash", columnList = "token_hash"),

		@Index(name = "idx_refresh_token_family", columnList = "family_id"),

		@Index(name = "idx_refresh_token_user", columnList = "user_id"),

		@Index(name = "idx_refresh_token_expires", columnList = "expires_at") })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

	// =========================================================
	// ID
	// =========================================================

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// =========================================================
	// TOKEN HASH
	// =========================================================

	/*
	 * Raw refresh token is NEVER stored.
	 *
	 * SHA-256 hash is stored instead.
	 */
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	// =========================================================
	// TOKEN FAMILY
	// =========================================================

	/*
	 * All rotated tokens belonging to the same login/session share the same family
	 * ID.
	 */
	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	// =========================================================
	// USER
	// =========================================================

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// =========================================================
	// CREATED AT
	// =========================================================

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	// =========================================================
	// EXPIRES AT
	// =========================================================

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	// =========================================================
	// REVOKED
	// =========================================================

	@Column(nullable = false)
	private boolean revoked;

	// =========================================================
	// REVOKED AT
	// =========================================================

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	// =========================================================
	// REVOCATION REASON
	// =========================================================

	@Enumerated(EnumType.STRING)
	@Column(name = "revocation_reason", length = 30)
	private RevocationReason revocationReason;

	// =========================================================
	// REPLACED BY
	// =========================================================

	/*
	 * Stores the HASH of the replacement refresh token.
	 *
	 * Example:
	 *
	 * Token A ↓ replacedByHash = hash(Token B)
	 */
	@Column(name = "replaced_by_hash", length = 64)
	private String replacedByHash;

	// =========================================================
	// OPTIMISTIC LOCKING
	// =========================================================

	@Version
	private Long version;

	// =========================================================
	// EXPIRED
	// =========================================================

	public boolean isExpired() {

		return !expiresAt.isAfter(LocalDateTime.now());
	}

	// =========================================================
	// VALID
	// =========================================================

	public boolean isValid() {

		return !revoked && !isExpired();
	}
}