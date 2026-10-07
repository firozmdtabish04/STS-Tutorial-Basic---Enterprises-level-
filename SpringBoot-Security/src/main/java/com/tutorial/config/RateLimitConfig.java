package com.tutorial.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

	// Login
	public static final int LOGIN_CAPACITY = 5;
	public static final int LOGIN_REFILL_TOKENS = 5;
	public static final int LOGIN_REFILL_SECONDS = 60;

	// Register
	public static final int REGISTER_CAPACITY = 3;
	public static final int REGISTER_REFILL_TOKENS = 3;
	public static final int REGISTER_REFILL_SECONDS = 60;

	// Refresh Token
	public static final int REFRESH_CAPACITY = 10;
	public static final int REFRESH_REFILL_TOKENS = 10;
	public static final int REFRESH_REFILL_SECONDS = 60;
}