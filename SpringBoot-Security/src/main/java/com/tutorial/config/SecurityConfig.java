package com.tutorial.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	private final CustomAuthenticationProvider authenticationProvider;

	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	private final JwtAccessDeniedHandler accessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				// =====================================================
				// CSRF
				// =====================================================
				/*
				 * Disabled because this is a stateless REST API using Authorization: Bearer
				 * JWT.
				 *
				 * If authentication is later moved to cookies, CSRF protection must be
				 * reconsidered.
				 */
				.csrf(csrf -> csrf.disable())

				// =====================================================
				// CORS
				// =====================================================
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// =====================================================
				// SESSION
				// =====================================================
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// =====================================================
				// REQUEST CACHE
				// =====================================================
				.requestCache(requestCache -> requestCache.disable())

				// =====================================================
				// AUTHENTICATION PROVIDER
				// =====================================================
				.authenticationProvider(authenticationProvider)

				// =====================================================
				// EXCEPTION HANDLING
				// =====================================================
				.exceptionHandling(exception -> exception

						// 401
						.authenticationEntryPoint(authenticationEntryPoint)

						// 403
						.accessDeniedHandler(accessDeniedHandler))

				// =====================================================
				// SECURITY HEADERS
				// =====================================================
				.headers(headers -> headers

						/*
						 * Prevent MIME type sniffing.
						 */
						.contentTypeOptions(contentTypeOptions -> {
						})

						/*
						 * Prevent clickjacking.
						 *
						 * sameOrigin is useful if Swagger UI is served by this application.
						 */
						.frameOptions(frame -> frame.sameOrigin())

						/*
						 * Control Referer information.
						 */
						.referrerPolicy(referrer -> referrer
								.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))

				// =====================================================
				// AUTHORIZATION
				// =====================================================
				.authorizeHttpRequests(auth -> auth

						// -------------------------------------------------
						// Swagger / OpenAPI
						// -------------------------------------------------
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						// -------------------------------------------------
						// Authentication
						// -------------------------------------------------
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

						// -------------------------------------------------
						// Public API
						// -------------------------------------------------
						.requestMatchers("/api/public/**").permitAll()

						// -------------------------------------------------
						// OPTIONS - CORS preflight
						// -------------------------------------------------
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// -------------------------------------------------
						// ADMIN
						// -------------------------------------------------
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// -------------------------------------------------
						// USER
						// -------------------------------------------------
						.requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

						// -------------------------------------------------
						// Everything else
						// -------------------------------------------------
						.anyRequest().authenticated())

				// =====================================================
				// JWT FILTER
				// =====================================================
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// =============================================================
	// AUTHENTICATION MANAGER
	// =============================================================

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}

	// =============================================================
	// CORS
	// =============================================================

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		/*
		 * Development frontend.
		 *
		 * Production: Replace with your real frontend domain.
		 */
		configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));

		configuration.setExposedHeaders(List.of("Authorization"));

		/*
		 * If your application uses Authorization headers, credentials are usually not
		 * necessary.
		 *
		 * If you later use secure HttpOnly cookies, configure this carefully.
		 */
		configuration.setAllowCredentials(false);

		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}