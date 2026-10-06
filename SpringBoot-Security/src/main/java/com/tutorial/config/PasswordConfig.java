package com.tutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {

		/*
		 * BCrypt strength = 12
		 *
		 * Higher value: More computationally expensive Better resistance against
		 * brute-force attacks
		 *
		 * 12 is a reasonable starting point.
		 *
		 * Benchmark on your production infrastructure before choosing the final cost.
		 */
		return new BCryptPasswordEncoder(12);
	}
}