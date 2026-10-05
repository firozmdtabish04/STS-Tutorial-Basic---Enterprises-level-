package com.tutorial.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.dto.response.UserResponse;
import com.tutorial.security.CustomUserDetails;
import com.tutorial.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// Get currently logged-in user
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public UserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {

		return userService.getUserById(userDetails.getUser().getId());
	}
}