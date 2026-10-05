package com.tutorial.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.dto.request.LoginRequest;
import com.tutorial.dto.request.RefreshTokenRequest;
import com.tutorial.dto.request.RegisterRequest;
import com.tutorial.dto.response.LoginResponse;
import com.tutorial.dto.response.TokenResponse;
import com.tutorial.dto.response.UserResponse;
import com.tutorial.entity.RefreshToken;
import com.tutorial.entity.User;
import com.tutorial.security.CustomUserDetails;
import com.tutorial.security.jwt.JwtService;
import com.tutorial.security.jwt.RefreshTokenService;
import com.tutorial.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final RefreshTokenService refreshTokenService;

	private final JwtService jwtService;

	// ==========================================
	// REGISTER
	// ==========================================

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	// ==========================================
	// LOGIN
	// ==========================================

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

		return ResponseEntity.ok(authService.login(request));
	}

	// ==========================================
	// REFRESH TOKEN
	// ==========================================

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {

		// 1. Validate refresh token
		RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

		// 2. Get user
		User user = refreshToken.getUser();

		// 3. Create UserDetails
		CustomUserDetails userDetails = new CustomUserDetails(user);

		// 4. Generate new access token
		String newAccessToken = jwtService.generateToken(userDetails);

		// 5. Return new access token
		return ResponseEntity
				.ok(TokenResponse.builder().accessToken(newAccessToken).refreshToken(refreshToken.getToken())
						.tokenType("Bearer").expiresIn(jwtService.getExpiration()).build());
	}
}