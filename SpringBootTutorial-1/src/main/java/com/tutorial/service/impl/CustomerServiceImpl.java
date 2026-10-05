package com.tutorial.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tutorial.CustomerRepository.CustomerRepository;
import com.tutorial.dto.request.CustomerCreateRequest;
import com.tutorial.dto.request.CustomerUpdateRequest;
import com.tutorial.dto.response.CustomerResponse;
import com.tutorial.entity.Customer;
import com.tutorial.exception.ResourceNotFoundException;
import com.tutorial.mapper.CustomerMapper;
import com.tutorial.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

	private final CustomerRepository customerRepository;

	private final CustomerMapper customerMapper;

	// =========================================================
	// CREATE
	// POST /api/v1/customers
	// =========================================================

	@Override
	public CustomerResponse createCustomer(CustomerCreateRequest request) {

		Customer customer = customerMapper.toEntity(request);

		Customer savedCustomer = customerRepository.save(customer);

		return customerMapper.toResponse(savedCustomer);
	}

	// =========================================================
	// READ ALL
	// GET /api/v1/customers
	// =========================================================

	@Override
	public List<CustomerResponse> getAllCustomers() {

		return customerRepository.findAll().stream().map(customerMapper::toResponse).toList();
	}

	// =========================================================
	// READ ONE
	// GET /api/v1/customers/{id}
	// =========================================================

	@Override
	public CustomerResponse getCustomerById(Long id) {

		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		return customerMapper.toResponse(customer);
	}

	// =========================================================
	// UPDATE
	// PUT /api/v1/customers/{id}
	// =========================================================

	@Override
	public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {

		Customer existingCustomer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		existingCustomer.setFirstName(request.getFirstName());

		existingCustomer.setLastName(request.getLastName());

		existingCustomer.setEmail(request.getEmail());

		existingCustomer.setPhone(request.getPhone());

		if (request.getDateOfBirth() != null) {
			existingCustomer.setDateOfBirth(request.getDateOfBirth());
		}

		Customer updatedCustomer = customerRepository.save(existingCustomer);

		return customerMapper.toResponse(updatedCustomer);
	}

	// =========================================================
	// DELETE
	// DELETE /api/v1/customers/{id}
	// =========================================================

	@Override
	public void deleteCustomer(Long id) {

		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		customerRepository.delete(customer);
	}
}