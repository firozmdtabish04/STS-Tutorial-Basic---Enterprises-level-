package com.tutorial.dto.response;

import java.time.LocalDateTime;

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
public class LoginResponse {

	private String accessToken;

	private long expiresIn;

	private String refreshToken;

	private String tokenType;

	private UserResponse user;

	private LocalDateTime timestamp;
}