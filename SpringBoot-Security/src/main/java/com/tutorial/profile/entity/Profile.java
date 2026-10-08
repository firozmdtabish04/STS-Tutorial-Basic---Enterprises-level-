package com.tutorial.profile.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tutorial.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profiles", uniqueConstraints = { @UniqueConstraint(name = "uk_profile_user", columnNames = "user_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// =========================================================
	// USER
	// =========================================================

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	// =========================================================
	// PERSONAL INFORMATION
	// =========================================================

	@Column(length = 20)
	private String phone;

	private LocalDate dateOfBirth;

	@Column(length = 20)
	private String gender;

	// =========================================================
	// ADDRESS
	// =========================================================

	@Column(length = 500)
	private String address;

	@Column(length = 100)
	private String city;

	@Column(length = 100)
	private String state;

	@Column(length = 100)
	private String country;

	// =========================================================
	// PROFILE IMAGE
	// =========================================================

	private String profileImage;

	// =========================================================
	// AUDIT
	// =========================================================

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {

		LocalDateTime now = LocalDateTime.now();

		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {

		updatedAt = LocalDateTime.now();
	}
}