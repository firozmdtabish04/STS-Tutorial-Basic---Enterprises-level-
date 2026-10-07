package com.tutorial.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutorial.entity.User;
import com.tutorial.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserRepository userRepository;

	// =========================================================
	// UNLOCK USER
	// =========================================================

	@Transactional
	public void unlockUser(Long userId) {

		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

		user.unlockAccount();

		userRepository.save(user);
	}
}