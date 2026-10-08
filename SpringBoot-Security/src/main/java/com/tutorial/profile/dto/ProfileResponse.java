package com.tutorial.profile.dto;

import java.time.LocalDate;

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
public class ProfileResponse {

	private Long id;

	private Long userId;

	private String email;

	private String firstName;

	private String lastName;

	private String phone;

	private LocalDate dateOfBirth;

	private String gender;

	private String address;

	private String city;

	private String state;

	private String country;

	private String profileImage;
}