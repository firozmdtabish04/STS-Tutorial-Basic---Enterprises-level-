package com.tutorial.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Table(name = "refresh_tokens", indexes = { @Index(name = "idx_refresh_token_hash", columnList = "token_hash"),
		@Index(name = "idx_refresh_token_family", columnList = "family_id"),
		@Index(name = "idx_refresh_token_user", columnList = "user_id"),
		@Index(name = "idx_refresh_token_expires", columnList = "expires_at") })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(nullable = false)
	private boolean revoked;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "replaced_by_hash", length = 64)
	private String replacedByHash;

	@Version
	private Long version;

	public boolean isExpired() {
		return expiresAt.isBefore(LocalDateTime.now());
	}

	public boolean isValid() {
		return !revoked && !isExpired();
	}
}