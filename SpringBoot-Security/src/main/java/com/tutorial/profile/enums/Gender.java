package com.tutorial.profile.enums;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {

	MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY;

	@JsonCreator
	public static Gender fromValue(String value) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Gender cannot be empty");
		}

		return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
	}
}