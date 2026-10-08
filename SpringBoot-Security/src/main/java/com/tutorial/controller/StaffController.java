package com.tutorial.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

	@GetMapping("/dashboard")
	public ResponseEntity<String> dashboard() {

		return ResponseEntity.ok("Welcome to Staff Dashboard");
	}

	@GetMapping("/profile")
	public ResponseEntity<String> profile() {

		return ResponseEntity.ok("Staff Profile");
	}

	@GetMapping("/tasks")
	public ResponseEntity<String> tasks() {

		return ResponseEntity.ok("Staff Tasks");
	}
}