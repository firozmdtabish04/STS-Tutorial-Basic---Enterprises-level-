package com.tutorial.CustomerRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tutorial.entity.Customer;
import com.tutorial.enums.CustomerStatus;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

	// =========================================================
	// DERIVED QUERY METHODS
	// =========================================================

	// findBy
	Optional<Customer> findByEmail(String email);

	Optional<Customer> findByCustomerCode(String customerCode);

	Optional<Customer> findByPhone(String phone);

	// And
	Optional<Customer> findByEmailAndPhone(String email, String phone);

	// Or
	List<Customer> findByFirstNameOrLastName(String firstName, String lastName);

	// Status
	List<Customer> findByStatus(CustomerStatus status);

	// =========================================================
	// STRING SEARCH
	// =========================================================

	// Containing
	List<Customer> findByFirstNameContaining(String name);

	// Containing + IgnoreCase
	List<Customer> findByFirstNameContainingIgnoreCase(String name);

	// StartingWith
	List<Customer> findByFirstNameStartingWith(String prefix);

	// EndingWith
	List<Customer> findByLastNameEndingWith(String suffix);

	// Like
	List<Customer> findByEmailLike(String email);

	// =========================================================
	// EXISTS
	// =========================================================

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);

	boolean existsByCustomerCode(String customerCode);

	// =========================================================
	// COUNT
	// =========================================================

	long countByStatus(CustomerStatus status);

	// =========================================================
	// DELETE
	// =========================================================

	long deleteByEmail(String email);

	long deleteByStatus(CustomerStatus status);

	// =========================================================
	// PAGINATION
	// =========================================================

	Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

	// =========================================================
	// SORTING
	// =========================================================

	List<Customer> findByStatus(CustomerStatus status, Sort sort);

	// =========================================================
	// JPQL @QUERY
	// =========================================================

	@Query("""
			    SELECT c
			    FROM Customer c
			    WHERE c.email = :email
			""")
	Optional<Customer> findCustomerUsingJPQL(@Param("email") String email);

	// Search by first name or last name
	@Query("""
			    SELECT c
			    FROM Customer c
			    WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
			""")
	List<Customer> searchCustomers(@Param("keyword") String keyword);

	// =========================================================
	// NATIVE QUERY
	// =========================================================

	@Query(value = """
			    SELECT *
			    FROM customers
			    WHERE status = :status
			""", nativeQuery = true)
	List<Customer> findByStatusNative(@Param("status") String status);

	// =========================================================
	// MODIFYING QUERY
	// =========================================================

	@Modifying
	@Query("""
			    UPDATE Customer c
			    SET c.status = :status
			    WHERE c.id = :id
			""")
	int updateCustomerStatus(@Param("id") Long id, @Param("status") CustomerStatus status);

	// =========================================================
	// ENTITY GRAPH
	// =========================================================

	/*
	 * Example only if Customer has:
	 * 
	 * @OneToMany(mappedBy = "customer") private List<Account> accounts;
	 * 
	 */

	/*
	 * @EntityGraph(attributePaths = {"accounts"}) Optional<Customer>
	 * findWithAccountsById(Long id);
	 */
}