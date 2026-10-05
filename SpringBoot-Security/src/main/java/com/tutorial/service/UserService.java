package com.tutorial.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tutorial.dto.response.UserResponse;
import com.tutorial.entity.User;
import com.tutorial.exception.ResourceNotFoundException;
import com.tutorial.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	// Get all users
	public List<UserResponse> getAllUsers() {

		return userRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	// Get user by ID
	public UserResponse getUserById(Long id) {

		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		return mapToResponse(user);
	}

	// Convert Entity → DTO
	private UserResponse mapToResponse(User user) {

		return UserResponse.builder().id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
				.email(user.getEmail()).role(user.getRole()).build();
	}
}