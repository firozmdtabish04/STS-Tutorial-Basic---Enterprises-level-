package com.tutorial.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {

	USER, STAFF, MANAGER, EMPLOYEE, ADMIN;

	@JsonCreator
	public static Role fromValue(String value) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Role cannot be empty");
		}

		return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
	}
}