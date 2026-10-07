package com.tutorial.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.api.StatefulRedisConnection;

@Configuration
public class Bucket4jConfig {

	@Bean
	public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> redisConnection) {

		return Bucket4jLettuce.casBasedBuilder(redisConnection)
				.expirationAfterWrite(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
						.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(2)))
				.build();
	}
}