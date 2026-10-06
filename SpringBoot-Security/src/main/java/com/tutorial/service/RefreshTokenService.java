package com.tutorial.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.entity.RefreshToken;
import com.tutorial.entity.User;
import com.tutorial.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	private static final int TOKEN_BYTES = 64;

	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${app.jwt.refresh-expiration}")
	private long refreshExpiration;

	// =========================================================
	// CREATE REFRESH TOKEN
	// =========================================================

	@Transactional
	public CreationResult createRefreshToken(User user) {

		LocalDateTime now = LocalDateTime.now();

		// Generate secure random raw token
		String rawToken = generateSecureToken();

		// Never store raw token in database
		String tokenHash = hashToken(rawToken);

		// Every login/session gets its own family
		String familyId = UUID.randomUUID().toString();

		RefreshToken refreshToken = RefreshToken.builder().tokenHash(tokenHash).familyId(familyId).user(user)
				.createdAt(now).expiresAt(now.plus(Duration.ofMillis(refreshExpiration))).revoked(false).build();

		refreshTokenRepository.save(refreshToken);

		/*
		 * IMPORTANT:
		 *
		 * rawToken -> returned to client tokenHash -> stored in database
		 *
		 * We NEVER store rawToken.
		 */

		return new CreationResult(rawToken);
	}

	// =========================================================
	// VERIFY REFRESH TOKEN
	// =========================================================

	@Transactional(readOnly = true)
	public User verifyRefreshToken(String rawToken) {

		validateRawToken(rawToken);

		String tokenHash = hashToken(rawToken);

		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		// Reuse detection
		if (refreshToken.isRevoked()) {

			throw new RefreshTokenReuseException("Refresh token reuse detected");
		}

		// Expiration
		if (refreshToken.isExpired()) {

			throw new IllegalArgumentException("Refresh token expired");
		}

		return refreshToken.getUser();
	}

	// =========================================================
	// ROTATE REFRESH TOKEN
	// =========================================================

	@Transactional(noRollbackFor = RefreshTokenReuseException.class)
	public RotationResult rotate(String rawToken) {

		validateRawToken(rawToken);

		// Hash incoming token
		String oldTokenHash = hashToken(rawToken);

		/*
		 * PESSIMISTIC WRITE LOCK
		 *
		 * Prevents two simultaneous requests from successfully rotating the same
		 * refresh token.
		 */
		RefreshToken oldToken = refreshTokenRepository.findByTokenHashForUpdate(oldTokenHash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		LocalDateTime now = LocalDateTime.now();

		// =====================================================
		// REUSE DETECTION
		// =====================================================

		if (oldToken.isRevoked()) {

			/*
			 * Someone is trying to reuse an already rotated or revoked refresh token.
			 *
			 * Revoke the complete token family.
			 */
			refreshTokenRepository.revokeFamily(oldToken.getFamilyId(), now);

			/*
			 * VERY IMPORTANT:
			 *
			 * noRollbackFor prevents the family revocation from being rolled back when this
			 * exception is thrown.
			 */
			throw new RefreshTokenReuseException("Refresh token reuse detected");
		}

		// =====================================================
		// EXPIRATION
		// =====================================================

		if (oldToken.isExpired()) {

			/*
			 * No need to mark it revoked.
			 *
			 * The expiration itself makes the token invalid.
			 */
			throw new IllegalArgumentException("Refresh token expired");
		}

		// =====================================================
		// GENERATE NEW REFRESH TOKEN
		// =====================================================

		String newRawToken = generateSecureToken();

		String newTokenHash = hashToken(newRawToken);

		// =====================================================
		// CREATE NEW TOKEN
		// =====================================================

		RefreshToken newToken = RefreshToken.builder().tokenHash(newTokenHash)

				/*
				 * IMPORTANT:
				 *
				 * Same family ID.
				 *
				 * This allows reuse detection to revoke the entire refresh-token family.
				 */
				.familyId(oldToken.getFamilyId())

				.user(oldToken.getUser())

				.createdAt(now)

				.expiresAt(now.plus(Duration.ofMillis(refreshExpiration)))

				.revoked(false)

				.build();

		// =====================================================
		// REVOKE OLD TOKEN
		// =====================================================

		oldToken.setRevoked(true);
		oldToken.setRevokedAt(now);

		/*
		 * Store hash of replacement token.
		 *
		 * Never store the raw token.
		 */
		oldToken.setReplacedByHash(newTokenHash);

		// =====================================================
		// SAVE
		// =====================================================

		refreshTokenRepository.save(oldToken);

		refreshTokenRepository.save(newToken);

		// =====================================================
		// RETURN
		// =====================================================

		return new RotationResult(newToken.getUser(), newRawToken);
	}

	// =========================================================
	// REVOKE ONE TOKEN
	// =========================================================

	@Transactional
	public void revokeToken(String rawToken) {

		if (rawToken == null || rawToken.isBlank()) {
			return;
		}

		String tokenHash = hashToken(rawToken);

		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {

			if (!token.isRevoked()) {

				token.setRevoked(true);
				token.setRevokedAt(LocalDateTime.now());

				refreshTokenRepository.save(token);
			}
		});
	}

	// =========================================================
	// REVOKE ALL USER TOKENS
	// =========================================================

	@Transactional
	public void revokeAllUserTokens(User user) {

		List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(user.getId());

		LocalDateTime now = LocalDateTime.now();

		for (RefreshToken token : tokens) {

			if (!token.isRevoked()) {

				token.setRevoked(true);
				token.setRevokedAt(now);
			}
		}

		refreshTokenRepository.saveAll(tokens);
	}

	// =========================================================
	// GENERATE SECURE RANDOM TOKEN
	// =========================================================

	private String generateSecureToken() {

		byte[] randomBytes = new byte[TOKEN_BYTES];

		secureRandom.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	// =========================================================
	// SHA-256 HASH
	// =========================================================

	private String hashToken(String rawToken) {

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

			StringBuilder hexString = new StringBuilder();

			for (byte b : hash) {

				String hex = Integer.toHexString(0xff & b);

				if (hex.length() == 1) {
					hexString.append('0');
				}

				hexString.append(hex);
			}

			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {

			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	// =========================================================
	// VALIDATE RAW TOKEN
	// =========================================================

	private void validateRawToken(String rawToken) {

		if (rawToken == null || rawToken.isBlank()) {

			throw new IllegalArgumentException("Refresh token is required");
		}
	}

	// =========================================================
	// RECORDS
	// =========================================================

	public record CreationResult(String rawToken) {
	}

	public record RotationResult(User user, String refreshToken) {
	}

	// =========================================================
	// REFRESH TOKEN REUSE EXCEPTION
	// =========================================================

	public static class RefreshTokenReuseException extends RuntimeException {

		public RefreshTokenReuseException(String message) {
			super(message);
		}
	}
}