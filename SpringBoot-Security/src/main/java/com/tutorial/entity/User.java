package com.tutorial.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "uk_users_email", columnNames = "email") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// =========================================================
	// BASIC INFORMATION
	// =========================================================

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	// =========================================================
	// ROLE
	// =========================================================

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Role role = Role.USER;

	// =========================================================
	// ACCOUNT STATUS
	// =========================================================

	@Column(nullable = false)
	@Builder.Default
	private boolean enabled = true;

	@Column(nullable = false)
	@Builder.Default
	private boolean locked = false;

	// =========================================================
	// FAILED LOGIN
	// =========================================================

	@Column(name = "failed_login_attempts", nullable = false)
	@Builder.Default
	private int failedLoginAttempts = 0;

	@Column(name = "locked_at")
	private LocalDateTime lockedAt;

	// =========================================================
	// FAILED LOGIN METHODS
	// =========================================================

	public void incrementFailedLoginAttempts() {
		this.failedLoginAttempts++;
	}

	public void resetFailedLoginAttempts() {
		this.failedLoginAttempts = 0;
	}

	// =========================================================
	// ACCOUNT LOCK
	// =========================================================

	public void lockAccount() {
		this.locked = true;
		this.lockedAt = LocalDateTime.now();
	}

	// =========================================================
	// ACCOUNT UNLOCK
	// =========================================================

	public void unlockAccount() {
		this.locked = false;
		this.lockedAt = null;
		this.failedLoginAttempts = 0;
	}
}