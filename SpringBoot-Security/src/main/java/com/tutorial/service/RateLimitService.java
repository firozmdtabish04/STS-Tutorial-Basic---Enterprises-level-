package com.tutorial.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimitService {

	private final ProxyManager<String> proxyManager;

	// =========================================================
	// LOGIN
	// 5 requests per minute
	// =========================================================

	public ConsumptionProbe tryLogin(String ipAddress) {

		Bucket bucket = getBucket("login:" + ipAddress, 5, 5, Duration.ofMinutes(1));

		return bucket.tryConsumeAndReturnRemaining(1);
	}

	// =========================================================
	// REGISTER
	// 3 requests per minute
	// =========================================================

	public ConsumptionProbe tryRegister(String ipAddress) {

		Bucket bucket = getBucket("register:" + ipAddress, 3, 3, Duration.ofMinutes(1));

		return bucket.tryConsumeAndReturnRemaining(1);
	}

	// =========================================================
	// REFRESH
	// 10 requests per minute
	// =========================================================

	public ConsumptionProbe tryRefresh(String ipAddress) {

		Bucket bucket = getBucket("refresh:" + ipAddress, 10, 10, Duration.ofMinutes(1));

		return bucket.tryConsumeAndReturnRemaining(1);
	}

	// =========================================================
	// CREATE / GET BUCKET
	// =========================================================

	private Bucket getBucket(String key, long capacity, long refillTokens, Duration refillDuration) {

		BucketConfiguration configuration = BucketConfiguration.builder()
				.addLimit(limit -> limit.capacity(capacity).refillIntervally(refillTokens, refillDuration)).build();

		return proxyManager.getProxy(key, () -> configuration);
	}
}