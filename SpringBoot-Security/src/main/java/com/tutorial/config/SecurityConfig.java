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
				// =====================================================
				// CSRF
				// =====================================================
				.csrf(csrf -> csrf.disable())

				// =====================================================
				// CORS
				// =====================================================
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// =====================================================
				// STATELESS
				// =====================================================
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// =====================================================
				// EXCEPTION HANDLING
				// =====================================================
				.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))

				// =====================================================
				// AUTHORIZATION
				// =====================================================
				.authorizeHttpRequests(auth -> auth

						// -------------------------------------------------
						// SWAGGER
						// -------------------------------------------------
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
						// -------------------------------------------------
						// AUTH
						// -------------------------------------------------
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

						// -------------------------------------------------
						// PUBLIC
						// -------------------------------------------------
						.requestMatchers("/api/public/**").permitAll()

						// -------------------------------------------------
						// CORS PREFLIGHT
						// -------------------------------------------------
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// -------------------------------------------------
						// ADMIN
						// -------------------------------------------------
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// -------------------------------------------------
						// EMPLOYEE
						// -------------------------------------------------
						.requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE", "ADMIN")

						// -------------------------------------------------
						// USER
						// -------------------------------------------------
						.requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
						// -------------------------------------------------
						// STAFF
						// -------------------------------------------------

						.requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")

						.requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")

						// -------------------------------------------------
						// PROFILE
						// -------------------------------------------------
						.requestMatchers("/api/profile/**").authenticated()

						// -------------------------------------------------
						// EVERYTHING ELSE
						// -------------------------------------------------
						.anyRequest().authenticated())

				// =====================================================
				// CUSTOM FILTERS
				// =====================================================
				.addFilterBefore(rateLimitFilter,
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

				.addFilterBefore(jwtAuthenticationFilter,
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

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
		 * JWT is sent using:
		 *
		 * Authorization: Bearer <access-token>
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