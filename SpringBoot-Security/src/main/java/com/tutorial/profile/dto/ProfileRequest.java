package com.tutorial.profile.dto;

import java.time.LocalDate;

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

	@Size(max = 20)
	private String phone;

	private LocalDate dateOfBirth;

	@Size(max = 20)
	private String gender;

	@Size(max = 500)
	private String address;

	@Size(max = 100)
	private String city;

	@Size(max = 100)
	private String state;

	@Size(max = 100)
	private String country;

	private String profileImage;
}