package com.tutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tutorial.security.CustomAuthenticationProvider;
import com.tutorial.security.jwt.JwtAuthenticationEntryPoint;
import com.tutorial.security.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	private final CustomAuthenticationProvider authenticationProvider;

	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				// ==========================================
				// CSRF
				// ==========================================
				.csrf(csrf -> csrf.disable())

				// ==========================================
				// SESSION
				// ==========================================
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// ==========================================
				// AUTHENTICATION PROVIDER
				// ==========================================
				.authenticationProvider(authenticationProvider)

				// ==========================================
				// EXCEPTION HANDLING
				// ==========================================
				.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))

				// ==========================================
				// AUTHORIZATION
				// ==========================================
				.authorizeHttpRequests(auth -> auth

						// --------------------------------------
						// Swagger / OpenAPI
						// --------------------------------------
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						// --------------------------------------
						// Authentication
						// --------------------------------------
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

						// --------------------------------------
						// Admin
						// --------------------------------------
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// --------------------------------------
						// User
						// --------------------------------------
						.requestMatchers("/api/user/**").authenticated()

						// --------------------------------------
						// Everything else
						// --------------------------------------
						.anyRequest().authenticated())

				// ==========================================
				// JWT FILTER
				// ==========================================
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// ==========================================
	// AUTHENTICATION MANAGER
	// ==========================================

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}
}