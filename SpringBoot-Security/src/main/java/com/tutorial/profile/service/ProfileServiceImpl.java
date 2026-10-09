
package com.tutorial.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.entity.User;
import com.tutorial.profile.dto.ProfileRequest;
import com.tutorial.profile.dto.ProfileResponse;
import com.tutorial.profile.entity.Profile;
import com.tutorial.profile.exception.ProfileAlreadyExistsException;
import com.tutorial.profile.exception.ProfileNotFoundException;
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

		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		var existingProfile = profileRepository.findByUserId(userId);

		if (existingProfile.isPresent()) {

			Profile profile = existingProfile.get();

			// An active profile already exists
			if (!profile.isDeleted()) {
				throw new ProfileAlreadyExistsException("Profile already exists for this user");
			}

			// Restore soft-deleted profile
			profile.restore();

			updateProfileFields(profile, request);

			Profile restoredProfile = profileRepository.save(profile);

			return mapToResponse(restoredProfile);
		}

		// Create a new profile
		Profile profile = Profile.builder().user(user).phone(request.getPhone()).dateOfBirth(request.getDateOfBirth())
				.gender(request.getGender()).address(request.getAddress()).city(request.getCity())
				.state(request.getState()).country(request.getCountry()).profileImage(request.getProfileImage())
				.deleted(false).build();

		Profile savedProfile = profileRepository.save(profile);

		return mapToResponse(savedProfile);
	}

	// =========================================================
	// GET MY PROFILE
	// =========================================================

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse getMyProfile(Long userId) {

		Profile profile = profileRepository.findByUserIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

		return mapToResponse(profile);
	}

	// =========================================================
	// UPDATE MY PROFILE
	// =========================================================

	@Override
	@Transactional
	public ProfileResponse updateProfile(Long userId, ProfileRequest request) {

		Profile profile = profileRepository.findByUserIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

		updateProfileFields(profile, request);

		Profile updatedProfile = profileRepository.save(profile);

		return mapToResponse(updatedProfile);
	}

	// =========================================================
	// DELETE MY PROFILE - SOFT DELETE
	// =========================================================

	@Override
	@Transactional
	public void deleteProfile(Long userId) {

		Profile profile = profileRepository.findByUserIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

		profile.softDelete();

		profileRepository.save(profile);
	}

	// =========================================================
	// GET PROFILE BY ID
	// =========================================================

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse getProfileById(Long id) {

		Profile profile = profileRepository.findById(id).filter(p -> !p.isDeleted())
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found with id: " + id));

		return mapToResponse(profile);
	}

	// =========================================================
	// UPDATE PROFILE BY ID
	// =========================================================

	@Override
	@Transactional
	public ProfileResponse updateProfileById(Long id, ProfileRequest request) {

		Profile profile = profileRepository.findById(id).filter(p -> !p.isDeleted())
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found with id: " + id));

		updateProfileFields(profile, request);

		Profile updatedProfile = profileRepository.save(profile);

		return mapToResponse(updatedProfile);
	}

	// =========================================================
	// UPDATE PROFILE FIELDS
	// =========================================================

	private void updateProfileFields(Profile profile, ProfileRequest request) {

		profile.setPhone(request.getPhone());
		profile.setDateOfBirth(request.getDateOfBirth());
		profile.setGender(request.getGender());
		profile.setAddress(request.getAddress());
		profile.setCity(request.getCity());
		profile.setState(request.getState());
		profile.setCountry(request.getCountry());
		profile.setProfileImage(request.getProfileImage());
	}

	// =========================================================
	// MAP ENTITY TO RESPONSE DTO
	// =========================================================

	private ProfileResponse mapToResponse(Profile profile) {

		User user = profile.getUser();

		return ProfileResponse.builder().id(profile.getId()).userId(user.getId()).email(user.getEmail())
				.firstName(user.getFirstName()).lastName(user.getLastName()).phone(profile.getPhone())
				.dateOfBirth(profile.getDateOfBirth()).gender(profile.getGender()).address(profile.getAddress())
				.city(profile.getCity()).state(profile.getState()).country(profile.getCountry())
				.profileImage(profile.getProfileImage()).version(profile.getVersion()).build();
	}
}