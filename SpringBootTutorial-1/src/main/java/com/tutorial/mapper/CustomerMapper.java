package com.tutorial.mapper;

import org.springframework.stereotype.Component;

import com.tutorial.dto.request.CustomerCreateRequest;
import com.tutorial.dto.response.CustomerResponse;
import com.tutorial.entity.Customer;

@Component
public class CustomerMapper {

	public Customer toEntity(CustomerCreateRequest request) {

		Customer customer = new Customer();

		customer.setFirstName(request.getFirstName());
		customer.setLastName(request.getLastName());
		customer.setEmail(request.getEmail());
		customer.setPhone(request.getPhone());
		customer.setDateOfBirth(request.getDateOfBirth());

		return customer;
	}

	public CustomerResponse toResponse(Customer customer) {

		return CustomerResponse.builder().id(customer.getId()).customerCode(customer.getCustomerCode())
				.firstName(customer.getFirstName()).lastName(customer.getLastName()).email(customer.getEmail())
				.phone(customer.getPhone()).dateOfBirth(customer.getDateOfBirth()).status(customer.getStatus()).build();
	}
}