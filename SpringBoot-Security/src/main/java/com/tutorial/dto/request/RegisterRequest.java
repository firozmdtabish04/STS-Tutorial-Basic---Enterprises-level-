package com.tutorial.dto.request;

import com.tutorial.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RegisterRequest {

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

	@NotNull
	private Role role;
}