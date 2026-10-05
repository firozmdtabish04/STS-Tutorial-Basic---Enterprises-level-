package com.tutorial.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.dto.request.LoginRequest;
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

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	@Transactional
	public UserResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.getEmail())) {

			throw new IllegalArgumentException("Email already registered");
		}

		User user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName())
				.email(request.getEmail().toLowerCase().trim()).password(passwordEncoder.encode(request.getPassword()))
				.role(User.Role.USER).enabled(true).locked(false).build();

		User savedUser = userRepository.save(user);

		return mapToResponse(savedUser);
	}

	public LoginResponse login(LoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
				.orElseThrow(() -> new RuntimeException("User not found"));

		CustomUserDetails userDetails = new CustomUserDetails(user);

		String token = jwtService.generateToken(userDetails);

		return LoginResponse.builder().token(token).tokenType("Bearer").expiresIn(jwtService.getExpiration())
				.user(mapToResponse(user)).build();
	}

	private UserResponse mapToResponse(User user) {

		return UserResponse.builder().id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
				.email(user.getEmail()).role(user.getRole()).build();
	}
}
