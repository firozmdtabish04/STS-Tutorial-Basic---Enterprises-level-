package com.tutorial.profile.dto;

import java.time.LocalDate;

import com.tutorial.profile.enums.Gender;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

	@Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
	private String phone;

	@Past(message = "Date of birth must be in the past")
	private LocalDate dateOfBirth;

	private Gender gender;

	@Size(max = 500, message = "Address cannot exceed 500 characters")
	private String address;

	@Size(max = 100, message = "City cannot exceed 100 characters")
	private String city;

	@Size(max = 100, message = "State cannot exceed 100 characters")
	private String state;

	@Size(max = 100, message = "Country cannot exceed 100 characters")
	private String country;

	@Size(max = 500, message = "Profile image URL cannot exceed 500 characters")
	private String profileImage;
}