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
import com.tutorial.enums.RevocationReason;
import com.tutorial.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	/*
	 * 64 bytes = 512 bits of random data.
	 */
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

		// Generate secure raw token
		String rawToken = generateSecureToken();

		// Hash raw token
		String tokenHash = hashToken(rawToken);

		/*
		 * Every login/session gets its own family.
		 */
		String familyId = UUID.randomUUID().toString();

		RefreshToken refreshToken = RefreshToken.builder().tokenHash(tokenHash).familyId(familyId).user(user)
				.createdAt(now).expiresAt(now.plus(Duration.ofMillis(refreshExpiration))).revoked(false).build();

		refreshTokenRepository.save(refreshToken);

		/*
		 * IMPORTANT:
		 *
		 * rawToken -> client tokenHash -> database
		 *
		 * Never store rawToken.
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

		// -----------------------------------------------------
		// REVOKED
		// -----------------------------------------------------

		if (refreshToken.isRevoked()) {

			/*
			 * If replacedByHash exists, this token was already rotated.
			 */

			if (refreshToken.getReplacedByHash() != null) {

				throw new RefreshTokenReuseException("Refresh token reuse detected. Please login again.");
			}

			throw new IllegalArgumentException("Refresh token has been revoked");
		}

		// -----------------------------------------------------
		// EXPIRED
		// -----------------------------------------------------

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

		// Hash incoming raw token
		String oldTokenHash = hashToken(rawToken);

		/*
		 * PESSIMISTIC WRITE LOCK
		 *
		 * Important for concurrent refresh requests.
		 */

		RefreshToken oldToken = refreshTokenRepository.findByTokenHashForUpdate(oldTokenHash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		LocalDateTime now = LocalDateTime.now();

		// =====================================================
		// REVOKED / REUSE DETECTION
		// =====================================================

		if (oldToken.isRevoked()) {

			/*
			 * If replacedByHash exists, this token was already rotated.
			 *
			 * Example:
			 *
			 * A -> B
			 *
			 * A is revoked.
			 *
			 * Someone sends A again.
			 *
			 * Possible token theft.
			 */

			if (oldToken.getReplacedByHash() != null) {

				refreshTokenRepository.revokeFamily(oldToken.getFamilyId(), now, RevocationReason.REUSE_DETECTED);

				throw new RefreshTokenReuseException("Refresh token reuse detected. Please login again.");
			}

			/*
			 * Normal logout/admin revocation.
			 */

			throw new IllegalArgumentException("Refresh token has been revoked");
		}

		// =====================================================
		// EXPIRATION
		// =====================================================

		if (oldToken.isExpired()) {

			throw new IllegalArgumentException("Refresh token expired");
		}

		// =====================================================
		// GENERATE NEW TOKEN
		// =====================================================

		String newRawToken = generateSecureToken();

		String newTokenHash = hashToken(newRawToken);

		// =====================================================
		// CREATE NEW TOKEN
		// =====================================================

		RefreshToken newToken = RefreshToken.builder().tokenHash(newTokenHash)

				/*
				 * Same family.
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

		oldToken.setRevocationReason(RevocationReason.ROTATED);

		/*
		 * Store hash of new token.
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
	public User revokeToken(String rawToken) {

		if (rawToken == null || rawToken.isBlank()) {
			return null;
		}

		String tokenHash = hashToken(rawToken);

		RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

		if (token == null) {
			return null;
		}

		// Get user before revoking
		User user = token.getUser();

		// Revoke only if not already revoked
		if (!token.isRevoked()) {

			LocalDateTime now = LocalDateTime.now();

			token.setRevoked(true);

			token.setRevokedAt(now);

			token.setRevocationReason(RevocationReason.LOGOUT);

			refreshTokenRepository.save(token);
		}

		return user;
	}

	// =========================================================
	// REVOKE ALL USER TOKENS
	// =========================================================

	@Transactional
	public void revokeAllUserTokens(User user) {

		if (user == null) {
			return;
		}

		List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(user.getId());

		LocalDateTime now = LocalDateTime.now();

		for (RefreshToken token : tokens) {

			if (!token.isRevoked()) {

				token.setRevoked(true);

				token.setRevokedAt(now);

				token.setRevocationReason(RevocationReason.ADMIN_REVOKED);
			}
		}

		refreshTokenRepository.saveAll(tokens);
	}

	// =========================================================
	// REVOKE TOKEN FAMILY
	// =========================================================

	@Transactional
	public int revokeTokenFamily(String familyId, RevocationReason reason) {

		if (familyId == null || familyId.isBlank()) {
			return 0;
		}

		if (reason == null) {

			throw new IllegalArgumentException("Revocation reason is required");
		}

		return refreshTokenRepository.revokeFamily(familyId, LocalDateTime.now(), reason);
	}

	// =========================================================
	// GENERATE SECURE TOKEN
	// =========================================================

	private String generateSecureToken() {

		byte[] randomBytes = new byte[TOKEN_BYTES];

		secureRandom.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	// =========================================================
	// SHA-256
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
	// REUSE EXCEPTION
	// =========================================================

	public static class RefreshTokenReuseException extends RuntimeException {

		public RefreshTokenReuseException(String message) {
			super(message);
		}
	}
}