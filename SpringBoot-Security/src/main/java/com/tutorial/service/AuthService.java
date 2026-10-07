package com.tutorial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.dto.request.LoginRequest;
import com.tutorial.dto.request.RefreshTokenRequest;
import com.tutorial.dto.request.RegisterRequest;
import com.tutorial.dto.response.LoginResponse;
import com.tutorial.dto.response.UserResponse;
import com.tutorial.entity.User;
import com.tutorial.repository.UserRepository;
import com.tutorial.security.CustomUserDetails;
import com.tutorial.security.jwt.JwtService;
import com.tutorial.security.logging.SecurityEvent;
import com.tutorial.security.logging.SecurityEventLogger;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final SecurityEventLogger securityEventLogger;

	@Value("${app.security.max-failed-attempts}")
	private int maxFailedAttempts;

	// =========================================================
	// REGISTER
	// =========================================================

	@Transactional
	public UserResponse register(RegisterRequest request) {

		String email = normalizeEmail(request.getEmail());

		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already registered");
		}

		User user = User.builder().firstName(request.getFirstName().trim()).lastName(request.getLastName().trim())
				.email(email).password(passwordEncoder.encode(request.getPassword())).role(User.Role.USER).enabled(true)
				.locked(false).failedLoginAttempts(0).build();

		User savedUser = userRepository.save(user);

		return mapToResponse(savedUser);
	}

	// =========================================================
	// LOGIN
	// =========================================================

	@Transactional
	public LoginResponse login(LoginRequest request) {

		String email = normalizeEmail(request.getEmail());

		// -----------------------------------------------------
		// 1. FIND USER
		// -----------------------------------------------------

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		// -----------------------------------------------------
		// 2. CHECK ACCOUNT STATUS
		// -----------------------------------------------------

		if (!user.isEnabled()) {

			throw new IllegalArgumentException("Account is disabled");
		}

		if (user.isLocked()) {

			throw new IllegalArgumentException("Account is locked");
		}

		// -----------------------------------------------------
		// 3. AUTHENTICATE USER
		// -----------------------------------------------------

		Authentication authentication;

		try {

			authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

		} catch (AuthenticationException ex) {

			// -------------------------------------------------
			// WRONG CREDENTIALS
			// -------------------------------------------------

			user.incrementFailedLoginAttempts();

			boolean accountLocked = false;

			// -------------------------------------------------
			// CHECK MAX FAILED ATTEMPTS
			// -------------------------------------------------

			if (user.getFailedLoginAttempts() >= maxFailedAttempts) {

				user.lockAccount();

				accountLocked = true;
			}

			userRepository.save(user);

			// -------------------------------------------------
			// SECURITY EVENT: LOGIN FAILED
			// -------------------------------------------------

			securityEventLogger.log(SecurityEvent.LOGIN_FAILED, user.getId(), user.getEmail(),
					"Invalid credentials. Failed attempts=" + user.getFailedLoginAttempts());

			// -------------------------------------------------
			// SECURITY EVENT: ACCOUNT LOCKED
			// -------------------------------------------------

			if (accountLocked) {

				securityEventLogger.log(SecurityEvent.ACCOUNT_LOCKED, user.getId(), user.getEmail(),
						"Maximum failed login attempts exceeded");
			}

			throw new IllegalArgumentException("Invalid email or password");
		}

		// -----------------------------------------------------
		// 4. SUCCESSFUL AUTHENTICATION
		// -----------------------------------------------------

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		user = userDetails.getUser();

		// -----------------------------------------------------
		// 5. RESET FAILED LOGIN ATTEMPTS
		// -----------------------------------------------------

		if (user.getFailedLoginAttempts() > 0) {

			user.resetFailedLoginAttempts();

			userRepository.save(user);
		}

		// -----------------------------------------------------
		// 6. GENERATE ACCESS TOKEN
		// -----------------------------------------------------

		String accessToken = jwtService.generateToken(userDetails);

		// -----------------------------------------------------
		// 7. CREATE REFRESH TOKEN
		// -----------------------------------------------------

		RefreshTokenService.CreationResult result = refreshTokenService.createRefreshToken(user);

		// -----------------------------------------------------
		// 8. SECURITY EVENT: LOGIN SUCCESS
		// -----------------------------------------------------

		securityEventLogger.log(SecurityEvent.LOGIN_SUCCESS, user.getId(), user.getEmail(),
				"User logged in successfully");

		// -----------------------------------------------------
		// 9. RETURN RESPONSE
		// -----------------------------------------------------

		return LoginResponse.builder().accessToken(accessToken).refreshToken(result.rawToken()).tokenType("Bearer")
				.expiresIn(jwtService.getExpiration()).user(mapToResponse(user)).build();
	}

	// =========================================================
	// REFRESH TOKEN
	// =========================================================

	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			throw new IllegalArgumentException("Refresh token is required");
		}

		// -----------------------------------------------------
		// ROTATE REFRESH TOKEN
		// -----------------------------------------------------

		RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.getRefreshToken());

		User user = result.user();

		// -----------------------------------------------------
		// SECURITY EVENT: TOKEN REFRESH
		// -----------------------------------------------------

		securityEventLogger.log(SecurityEvent.TOKEN_REFRESH, user.getId(), user.getEmail(),
				"Access token and refresh token rotated successfully");

		// -----------------------------------------------------
		// CREATE NEW USER DETAILS
		// -----------------------------------------------------

		CustomUserDetails userDetails = new CustomUserDetails(user);

		// -----------------------------------------------------
		// GENERATE NEW ACCESS TOKEN
		// -----------------------------------------------------

		String newAccessToken = jwtService.generateToken(userDetails);

		// -----------------------------------------------------
		// RETURN RESPONSE
		// -----------------------------------------------------

		return LoginResponse.builder().accessToken(newAccessToken).refreshToken(result.refreshToken())
				.tokenType("Bearer").expiresIn(jwtService.getExpiration()).user(mapToResponse(user)).build();
	}

	// =========================================================
	// LOGOUT
	// =========================================================

	@Transactional
	public void logout(RefreshTokenRequest request) {

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			return;
		}

		/*
		 * Revoke refresh token.
		 *
		 * We are not logging the refresh token itself. The refresh token must NEVER
		 * appear in application logs.
		 */

		refreshTokenService.revokeToken(request.getRefreshToken());
	}

	// =========================================================
	// LOGOUT ALL DEVICES
	// =========================================================

	// =========================================================
	// NORMALIZE EMAIL
	// =========================================================

	private String normalizeEmail(String email) {

		if (email == null || email.isBlank()) {

			throw new IllegalArgumentException("Email is required");
		}

		return email.trim().toLowerCase();
	}

	// =========================================================
	// USER RESPONSE
	// =========================================================

	private UserResponse mapToResponse(User user) {

		return UserResponse.builder().id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
				.email(user.getEmail()).role(user.getRole()).build();
	}
}