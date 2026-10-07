package com.tutorial.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.tutorial.security.CustomAuthenticationProvider;
import com.tutorial.security.jwt.JwtAccessDeniedHandler;
import com.tutorial.security.jwt.JwtAuthenticationEntryPoint;
import com.tutorial.security.jwt.JwtAuthenticationFilter;
import com.tutorial.security.ratelimit.RateLimitFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	private final CustomAuthenticationProvider authenticationProvider;

	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	private final JwtAccessDeniedHandler accessDeniedHandler;
	private final RateLimitFilter rateLimitFilter;

	// =========================================================
	// SECURITY FILTER CHAIN
	// =========================================================

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				// =================================================
				// CSRF
				// =================================================

				.csrf(csrf -> csrf.disable())

				// =================================================
				// CORS
				// =================================================

				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// =================================================
				// SESSION
				// =================================================

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// =================================================
				// REQUEST CACHE
				// =================================================

				.requestCache(requestCache -> requestCache.disable())

				// =================================================
				// AUTHENTICATION PROVIDER
				// =================================================

				.authenticationProvider(authenticationProvider)

				// =================================================
				// EXCEPTION HANDLING
				// =================================================

				.exceptionHandling(exception -> exception

						.authenticationEntryPoint(authenticationEntryPoint)

						.accessDeniedHandler(accessDeniedHandler))

				// =================================================
				// SECURITY HEADERS
				// =================================================

				.headers(headers -> headers

						// Prevent MIME-type sniffing
						.contentTypeOptions(contentTypeOptions -> {
						})

						// Prevent clickjacking
						.frameOptions(frame -> frame.sameOrigin())

						// Control Referer information
						.referrerPolicy(referrer -> referrer
								.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

						// Permissions Policy
						.permissionsPolicy(
								permissions -> permissions.policy("camera=(), microphone=(), geolocation=()")))
				// =================================================
				// AUTHORIZATION
				// =================================================

				.authorizeHttpRequests(auth -> auth

						// Swagger
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						// Authentication
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

						// Public APIs
						.requestMatchers("/api/public/**").permitAll()

						// CORS preflight
						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

						// ADMIN
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// USER + ADMIN
						.requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

						// Everything else
						.anyRequest().authenticated())

				// =================================================
				// JWT FILTER
				// =================================================
				// =========================================================
				// RATE LIMIT FILTER
				// =========================================================

				.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

				// =========================================================
				// JWT FILTER
				// =========================================================

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	// =========================================================
	// AUTHENTICATION MANAGER
	// =========================================================

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}

	// =========================================================
	// CORS CONFIGURATION
	// =========================================================

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		// =====================================================
		// ALLOWED ORIGINS
		// =====================================================

		configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

		// =====================================================
		// ALLOWED METHODS
		// =====================================================

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		// =====================================================
		// ALLOWED HEADERS
		// =====================================================

		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));

		// =====================================================
		// CREDENTIALS
		// =====================================================

		/*
		 * JWT is sent through:
		 *
		 * Authorization: Bearer <token>
		 *
		 * Therefore browser credentials are not required.
		 */
		configuration.setAllowCredentials(false);

		// =====================================================
		// PREFLIGHT CACHE
		// =====================================================

		configuration.setMaxAge(3600L);

		// =====================================================
		// REGISTER CORS
		// =====================================================

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}