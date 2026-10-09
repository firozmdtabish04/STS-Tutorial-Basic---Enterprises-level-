package com.tutorial.profile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.profile.dto.ProfileRequest;
import com.tutorial.profile.dto.ProfileResponse;
import com.tutorial.profile.service.ProfileService;
import com.tutorial.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	// =========================================================
	// CREATE
	// =========================================================

	@PostMapping("/create")
	public ResponseEntity<ProfileResponse> createProfile(Authentication authentication,
			@Valid @RequestBody ProfileRequest request) {

		Long userId = getUserId(authentication);

		ProfileResponse response = profileService.createProfile(userId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// =========================================================
	// READ
	// =========================================================

	@GetMapping("/retrieve")
	public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {

		Long userId = getUserId(authentication);

		ProfileResponse response = profileService.getMyProfile(userId);

		return ResponseEntity.ok(response);
	}

	// =========================================================
	// UPDATE
	// =========================================================

	@PutMapping("/update")
	public ResponseEntity<ProfileResponse> updateMyProfile(Authentication authentication,
			@Valid @RequestBody ProfileRequest request) {

		Long userId = getUserId(authentication);

		ProfileResponse response = profileService.updateProfile(userId, request);

		return ResponseEntity.ok(response);
	}

	// =========================================================
	// DELETE
	// =========================================================

	@DeleteMapping("/delete")
	public ResponseEntity<Void> deleteMyProfile(Authentication authentication) {

		Long userId = getUserId(authentication);

		profileService.deleteProfile(userId);

		return ResponseEntity.noContent().build();
	}

	// =========================================================
	// GET USER ID
	// =========================================================

	private Long getUserId(Authentication authentication) {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		return userDetails.getUser().getId();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {

		ProfileResponse response = profileService.getProfileById(id);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProfileResponse> updateProfileById(@PathVariable Long id,
			@Valid @RequestBody ProfileRequest request) {

		ProfileResponse response = profileService.updateProfileById(id, request);

		return ResponseEntity.ok(response);
	}

}