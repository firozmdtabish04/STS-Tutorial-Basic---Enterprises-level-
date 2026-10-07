package com.tutorial.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.dto.response.UserResponse;
import com.tutorial.service.AdminService;
import com.tutorial.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final UserService userService;

	private final AdminService adminService;

	// =========================================================
	// GET ALL USERS
	// =========================================================

	@GetMapping("/users")
	public List<UserResponse> getAllUsers() {

		return userService.getAllUsers();
	}

	// =========================================================
	// UNLOCK USER
	// =========================================================

	@PutMapping("/users/{userId}/unlock")
	public ResponseEntity<String> unlockUser(@PathVariable Long userId) {

		adminService.unlockUser(userId);

		return ResponseEntity.ok("User account unlocked successfully");
	}
}