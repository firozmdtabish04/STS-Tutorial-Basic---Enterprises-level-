package com.tutorial.service;

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

	// =========================================================
	// REGISTER
	// =========================================================

	@Transactional
	public UserResponse register(RegisterRequest request) {

		String email = normalizeEmail(request.getEmail());

		if (userRepository.existsByEmail(email)) {

			throw new IllegalArgumentException("Email already registered");
		}

		User user = User.builder()

				.firstName(request.getFirstName().trim())

				.lastName(request.getLastName().trim())

				.email(email)

				.password(passwordEncoder.encode(request.getPassword()))

				.role(User.Role.USER)

				.enabled(true)

				.locked(false)

				.failedLoginAttempts(0)

				.build();

		User savedUser = userRepository.save(user);

		return mapToResponse(savedUser);
	}

	// =========================================================
	// LOGIN
	// =========================================================

	@Transactional
	public LoginResponse login(LoginRequest request) {

		String email = normalizeEmail(request.getEmail());

		Authentication authentication;

		try {

			authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

		} catch (AuthenticationException ex) {

			/*
			 * Keep authentication error generic.
			 *
			 * Don't reveal whether the email exists.
			 */
			throw new IllegalArgumentException("Invalid email or password");
		}

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User user = userDetails.getUser();

		// =====================================================
		// GENERATE ACCESS TOKEN
		// =====================================================

		String accessToken = jwtService.generateToken(userDetails);

		// =====================================================
		// CREATE REFRESH TOKEN
		// =====================================================

		RefreshTokenService.CreationResult result = refreshTokenService.createRefreshToken(user);

		// =====================================================
		// RESPONSE
		// =====================================================

		return LoginResponse.builder()

				.accessToken(accessToken)

				.refreshToken(result.rawToken())

				.tokenType("Bearer")

				.expiresIn(jwtService.getExpiration())

				.user(mapToResponse(user))

				.build();
	}

	// =========================================================
	// REFRESH TOKEN
	// =========================================================

	/*
	 * No need to put the transaction here.
	 *
	 * RefreshTokenService.rotate() owns the transaction because it performs the
	 * pessimistic lock and token rotation.
	 */
	public LoginResponse refresh(RefreshTokenRequest request) {

		if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {

			throw new IllegalArgumentException("Refresh token is required");
		}

		// =====================================================
		// ROTATE REFRESH TOKEN
		// =====================================================

		RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.getRefreshToken());

		User user = result.user();

		// =====================================================
		// CREATE NEW ACCESS TOKEN
		// =====================================================

		CustomUserDetails userDetails = new CustomUserDetails(user);

		String newAccessToken = jwtService.generateToken(userDetails);

		// =====================================================
		// RETURN NEW TOKEN RESPONSE
		// =====================================================

		return LoginResponse.builder()

				.accessToken(newAccessToken)

				.refreshToken(result.refreshToken())

				.tokenType("Bearer")

				.expiresIn(jwtService.getExpiration())

				.user(mapToResponse(user))

				.build();
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

		return UserResponse.builder()

				.id(user.getId())

				.firstName(user.getFirstName())

				.lastName(user.getLastName())

				.email(user.getEmail())

				.role(user.getRole())

				.build();
	}
}