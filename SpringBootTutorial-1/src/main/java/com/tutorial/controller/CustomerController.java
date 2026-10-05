package com.tutorial.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.dto.request.CustomerCreateRequest;
import com.tutorial.dto.request.CustomerUpdateRequest;
import com.tutorial.dto.response.CustomerResponse;
import com.tutorial.service.CustomerService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	// =========================================================
	// CREATE
	// POST /api/v1/customers
	// =========================================================

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {

		CustomerResponse response = customerService.createCustomer(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// =========================================================
	// GET BY ID
	// GET /api/v1/customers/{id}
	// =========================================================

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> getById(@PathVariable(value = "id"

	) @Positive(message = "ID must be positive") Long id) {

		return ResponseEntity.ok(customerService.getCustomerById(id));
	}

	// =========================================================
	// UPDATE
	// PUT /api/v1/customers/{id}
	// =========================================================

	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponse> update(@PathVariable(value = "id"

	) @Positive(message = "ID must be positive") Long id,

			@Valid @RequestBody CustomerUpdateRequest request) {

		return ResponseEntity.ok(customerService.updateCustomer(id, request));
	}

	// =========================================================
	// DELETE
	// DELETE /api/v1/customers/{id}
	// =========================================================

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable(value = "id") @Positive(message = "ID must be positive") Long id) {

		customerService.deleteCustomer(id);

		return ResponseEntity.noContent().build();
	}
}