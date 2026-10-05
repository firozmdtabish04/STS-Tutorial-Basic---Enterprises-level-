package com.tutorial.security.jwt;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

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

	@Value("${app.jwt.refresh-expiration}")
	private long refreshExpiration;

	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public RefreshToken createRefreshToken(User user) {

		// Remove previous refresh tokens for this user
		refreshTokenRepository.deleteByUserId(user.getId());

		byte[] randomBytes = new byte[64];
		secureRandom.nextBytes(randomBytes);

		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

		RefreshToken refreshToken = RefreshToken.builder().token(token).user(user)
				.expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000)).revoked(false).build();

		return refreshTokenRepository.save(refreshToken);
	}

	public RefreshToken verifyRefreshToken(String token) {

		RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		if (!refreshToken.isValid()) {
			throw new IllegalArgumentException("Refresh token expired or revoked");
		}

		return refreshToken;
	}

	@Transactional
	public void revokeToken(String token) {

		RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		refreshToken.setRevoked(true);

		refreshTokenRepository.save(refreshToken);
	}
}