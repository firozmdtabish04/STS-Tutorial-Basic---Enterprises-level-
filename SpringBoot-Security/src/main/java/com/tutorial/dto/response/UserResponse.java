package com.tutorial.dto.response;

import com.tutorial.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private Role role;
}