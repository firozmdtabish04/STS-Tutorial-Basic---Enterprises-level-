package com.tutorial.dto.response;

import java.time.LocalDate;

import com.tutorial.enums.CustomerStatus;

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

public class CustomerResponse {

	private Long id;

	private String customerCode;

	private String firstName;

	private String lastName;

	private String email;

	private String phone;

	private LocalDate dateOfBirth;

	private CustomerStatus status;
}