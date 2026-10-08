package com.tutorial.profile.service;

import com.tutorial.profile.dto.ProfileRequest;
import com.tutorial.profile.dto.ProfileResponse;

public interface ProfileService {

	// CREATE
	ProfileResponse createProfile(Long userId, ProfileRequest request);

	// READ
	ProfileResponse getMyProfile(Long userId);

	// UPDATE
	ProfileResponse updateProfile(Long userId, ProfileRequest request);

	// DELETE
	void deleteProfile(Long userId);
}