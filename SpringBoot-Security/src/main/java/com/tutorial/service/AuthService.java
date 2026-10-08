package com.tutorial.service;

import java.time.LocalDateTime;

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
import com.tutorial.entity.Role;
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

	public UserResponse register(RegisterRequest request) {

		String email = request.getEmail().trim().toLowerCase();

		// Check duplicate email
		if (userRepository.existsByEmail(email)) {
			throw new RuntimeException("Email already registered");
		}

		// ADMIN cannot be registered publicly
		if (request.getRole() == Role.ADMIN) {
			throw new RuntimeException("ADMIN registration is not allowed");
		}

		// Create user
		User user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName()).email(email)
				.password(passwordEncoder.encode(request.getPassword())).role(request.getRole()).enabled(true)
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

			// -------------------------------------------------
			// SAVE FAILED ATTEMPT
			// -------------------------------------------------

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
		// 9. RETURN LOGIN RESPONSE
		// -----------------------------------------------------

		return LoginResponse.builder().accessToken(accessToken).expiresIn(jwtService.getExpiration())
				.refreshToken(result.rawToken()).tokenType("Bearer").user(mapToResponse(user))
				.timestamp(LocalDateTime.now()).build();
	}

	// =========================================================
	// REFRESH TOKEN
	// =========================================================

	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {

		// -----------------------------------------------------
		// 1. VALIDATE REQUEST
		// -----------------------------------------------------

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			throw new IllegalArgumentException("Refresh token is required");
		}

		// -----------------------------------------------------
		// 2. ROTATE REFRESH TOKEN
		// -----------------------------------------------------

		RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.getRefreshToken());

		User user = result.user();

		// -----------------------------------------------------
		// 3. SECURITY EVENT: TOKEN REFRESH
		// -----------------------------------------------------

		securityEventLogger.log(SecurityEvent.TOKEN_REFRESH, user.getId(), user.getEmail(),
				"Access token and refresh token rotated successfully");

		// -----------------------------------------------------
		// 4. CREATE USER DETAILS
		// -----------------------------------------------------

		CustomUserDetails userDetails = new CustomUserDetails(user);

		// -----------------------------------------------------
		// 5. GENERATE NEW ACCESS TOKEN
		// -----------------------------------------------------

		String newAccessToken = jwtService.generateToken(userDetails);

		// -----------------------------------------------------
		// 6. RETURN REFRESH RESPONSE
		// -----------------------------------------------------

		return LoginResponse.builder().accessToken(newAccessToken).expiresIn(jwtService.getExpiration())
				.refreshToken(result.refreshToken()).tokenType("Bearer").user(mapToResponse(user))
				.timestamp(LocalDateTime.now()).build();
	}

	// =========================================================
	// LOGOUT
	// =========================================================

	@Transactional
	public void logout(RefreshTokenRequest request) {

		// -----------------------------------------------------
		// 1. VALIDATE REQUEST
		// -----------------------------------------------------

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			return;
		}

		// -----------------------------------------------------
		// 2. REVOKE REFRESH TOKEN
		// -----------------------------------------------------

		User user = refreshTokenService.revokeToken(request.getRefreshToken());

		// -----------------------------------------------------
		// 3. SECURITY EVENT: LOGOUT
		// -----------------------------------------------------

		if (user != null) {

			securityEventLogger.log(SecurityEvent.LOGOUT, user.getId(), user.getEmail(),
					"User logged out successfully");
		}
	}

	// =========================================================
	// LOGOUT ALL DEVICES
	// =========================================================

	@Transactional
	public void logoutAll(Long userId) {

		// -----------------------------------------------------
		// 1. FIND USER
		// -----------------------------------------------------

		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

		// -----------------------------------------------------
		// 2. REVOKE ALL REFRESH TOKENS
		// -----------------------------------------------------

		refreshTokenService.revokeAllUserTokens(user);

		// -----------------------------------------------------
		// 3. SECURITY EVENT: LOGOUT ALL
		// -----------------------------------------------------

		securityEventLogger.log(SecurityEvent.LOGOUT_ALL, user.getId(), user.getEmail(), "All refresh tokens revoked");
	}

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