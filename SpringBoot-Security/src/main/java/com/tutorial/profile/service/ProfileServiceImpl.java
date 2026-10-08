package com.tutorial.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.entity.User;
import com.tutorial.profile.dto.ProfileRequest;
import com.tutorial.profile.dto.ProfileResponse;
import com.tutorial.profile.entity.Profile;
import com.tutorial.profile.repository.ProfileRepository;
import com.tutorial.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

	private final ProfileRepository profileRepository;
	private final UserRepository userRepository;

	// =========================================================
	// CREATE PROFILE
	// =========================================================

	@Override
	@Transactional
	public ProfileResponse createProfile(Long userId, ProfileRequest request) {

		// Check user
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Prevent duplicate profile
		if (profileRepository.existsByUserId(userId)) {
			throw new RuntimeException("Profile already exists");
		}

		// Create profile
		Profile profile = Profile.builder().user(user).phone(request.getPhone()).dateOfBirth(request.getDateOfBirth())
				.gender(request.getGender()).address(request.getAddress()).city(request.getCity())
				.state(request.getState()).country(request.getCountry()).profileImage(request.getProfileImage())
				.build();

		Profile savedProfile = profileRepository.save(profile);

		return mapToResponse(savedProfile);
	}

	// =========================================================
	// READ PROFILE
	// =========================================================

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse getMyProfile(Long userId) {

		Profile profile = profileRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Profile not found"));

		return mapToResponse(profile);
	}

	// =========================================================
	// UPDATE PROFILE
	// =========================================================

	@Override
	@Transactional
	public ProfileResponse updateProfile(Long userId, ProfileRequest request) {

		Profile profile = profileRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Profile not found"));

		profile.setPhone(request.getPhone());
		profile.setDateOfBirth(request.getDateOfBirth());
		profile.setGender(request.getGender());
		profile.setAddress(request.getAddress());
		profile.setCity(request.getCity());
		profile.setState(request.getState());
		profile.setCountry(request.getCountry());
		profile.setProfileImage(request.getProfileImage());

		Profile updatedProfile = profileRepository.save(profile);

		return mapToResponse(updatedProfile);
	}

	// =========================================================
	// DELETE PROFILE
	// =========================================================

	@Override
	@Transactional
	public void deleteProfile(Long userId) {

		Profile profile = profileRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Profile not found"));

		profileRepository.delete(profile);
	}

	// =========================================================
	// MAPPER
	// =========================================================

	private ProfileResponse mapToResponse(Profile profile) {

		User user = profile.getUser();

		return ProfileResponse.builder().id(profile.getId()).userId(user.getId()).email(user.getEmail())
				.firstName(user.getFirstName()).lastName(user.getLastName()).phone(profile.getPhone())
				.dateOfBirth(profile.getDateOfBirth()).gender(profile.getGender()).address(profile.getAddress())
				.city(profile.getCity()).state(profile.getState()).country(profile.getCountry())
				.profileImage(profile.getProfileImage()).build();
	}
}