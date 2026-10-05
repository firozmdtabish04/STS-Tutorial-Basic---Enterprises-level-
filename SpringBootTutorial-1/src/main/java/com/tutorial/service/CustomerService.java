package com.tutorial.service;

import java.util.List;

import com.tutorial.dto.request.CustomerCreateRequest;
import com.tutorial.dto.request.CustomerUpdateRequest;
import com.tutorial.dto.response.CustomerResponse;

public interface CustomerService {

	// CREATE
	CustomerResponse createCustomer(CustomerCreateRequest request);

	// READ ALL
	List<CustomerResponse> getAllCustomers();

	// READ ONE
	CustomerResponse getCustomerById(Long id);

	// UPDATE
	CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request);

	// DELETE
	void deleteCustomer(Long id);
}