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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

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
		// 1. Find user
		// -----------------------------------------------------

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		// -----------------------------------------------------
		// 2. Check account status
		// -----------------------------------------------------

		if (!user.isEnabled()) {

			throw new IllegalArgumentException("Account is disabled");
		}

		if (user.isLocked()) {

			throw new IllegalArgumentException("Account is locked");
		}

		// -----------------------------------------------------
		// 3. Authenticate username + password
		// -----------------------------------------------------

		Authentication authentication;

		try {

			authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

		} catch (AuthenticationException ex) {

			// -------------------------------------------------
			// WRONG PASSWORD
			// -------------------------------------------------

			user.incrementFailedLoginAttempts();

			// -------------------------------------------------
			// CHECK MAX ATTEMPTS
			// -------------------------------------------------

			if (user.getFailedLoginAttempts() >= maxFailedAttempts) {

				user.lockAccount();
			}

			userRepository.save(user);

			throw new IllegalArgumentException("Invalid email or password");
		}

		// -----------------------------------------------------
		// 4. SUCCESSFUL LOGIN
		// -----------------------------------------------------

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		user = userDetails.getUser();

		// -----------------------------------------------------
		// Reset failed attempts
		// -----------------------------------------------------

		if (user.getFailedLoginAttempts() > 0) {

			user.resetFailedLoginAttempts();

			userRepository.save(user);
		}

		// -----------------------------------------------------
		// 5. Generate access token
		// -----------------------------------------------------

		String accessToken = jwtService.generateToken(userDetails);

		// -----------------------------------------------------
		// 6. Create refresh token
		// -----------------------------------------------------

		RefreshTokenService.CreationResult result = refreshTokenService.createRefreshToken(user);

		// -----------------------------------------------------
		// 7. Return response
		// -----------------------------------------------------

		return LoginResponse.builder().accessToken(accessToken).refreshToken(result.rawToken()).tokenType("Bearer")
				.expiresIn(jwtService.getExpiration()).user(mapToResponse(user)).build();
	}

	// =========================================================
	// REFRESH TOKEN
	// =========================================================

	public LoginResponse refresh(RefreshTokenRequest request) {

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			throw new IllegalArgumentException("Refresh token is required");
		}

		RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.getRefreshToken());

		User user = result.user();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		String newAccessToken = jwtService.generateToken(userDetails);

		return LoginResponse.builder().accessToken(newAccessToken).refreshToken(result.refreshToken())
				.tokenType("Bearer").expiresIn(jwtService.getExpiration()).user(mapToResponse(user)).build();
	}

	// =========================================================
	// LOGOUT
	// =========================================================

	@Transactional
	public void logout(RefreshTokenRequest request) {

		if (request == null) {
			return;
		}

		refreshTokenService.revokeToken(request.getRefreshToken());
	}

	// =========================================================
	// LOGOUT ALL DEVICES
	// =========================================================

	@Transactional
	public void logoutAll(Long userId) {

		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

		refreshTokenService.revokeAllUserTokens(user);
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