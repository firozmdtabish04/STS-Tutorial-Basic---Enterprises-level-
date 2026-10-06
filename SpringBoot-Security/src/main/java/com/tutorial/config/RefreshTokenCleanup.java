package com.tutorial.config;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanup {

	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * Removes expired refresh tokens from the database.
	 *
	 * Default: Runs every 1 hour.
	 *
	 * Configure with:
	 *
	 * security.refresh-token.cleanup-rate=3600000
	 */
	@Scheduled(fixedRateString = "${security.refresh-token.cleanup-rate:3600000}", initialDelayString = "${security.refresh-token.cleanup-initial-delay:60000}")
	@Transactional
	public void cleanupExpiredTokens() {

		LocalDateTime now = LocalDateTime.now();

		try {

			int deleted = refreshTokenRepository.deleteExpiredTokens(now);

			if (deleted > 0) {

				log.info("Refresh token cleanup completed. " + "Deleted {} expired tokens.", deleted);

			} else {

				log.debug("Refresh token cleanup completed. " + "No expired tokens found.");
			}

		} catch (Exception exception) {

			/*
			 * Do not allow a scheduled maintenance failure to crash the application.
			 */
			log.error("Failed to clean up expired refresh tokens", exception);
		}
	}
}