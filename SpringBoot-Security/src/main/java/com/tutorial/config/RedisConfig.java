package com.tutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class RedisConfig {

	@Bean(destroyMethod = "shutdown")
	public RedisClient redisClient() {

		RedisURI redisURI = RedisURI.builder().withHost("localhost").withPort(6379).build();

		return RedisClient.create(redisURI);
	}

	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {

		RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

		return redisClient.connect(codec);
	}
}