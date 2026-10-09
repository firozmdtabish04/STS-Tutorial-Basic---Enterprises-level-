package com.tutorial.profile.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tutorial.entity.User;
import com.tutorial.profile.enums.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.persistence.Version;
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

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Gender gender;

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

	@Column(name = "profile_image", length = 500)
	private String profileImage;

	// =========================================================
	// AUDIT
	// =========================================================

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// =========================================================
	// OPTIMISTIC LOCKING
	// =========================================================

	@Version
	@Column(nullable = false)
	private Long version;

	// =========================================================
	// SOFT DELETE
	// =========================================================

	@Column(name = "deleted", nullable = false)
	@Builder.Default
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	// =========================================================
	// CREATE
	// =========================================================

	@PrePersist
	protected void onCreate() {

		LocalDateTime now = LocalDateTime.now();

		createdAt = now;
		updatedAt = now;
	}

	// =========================================================
	// UPDATE
	// =========================================================

	@PreUpdate
	protected void onUpdate() {

		updatedAt = LocalDateTime.now();
	}

	// =========================================================
	// SOFT DELETE
	// =========================================================

	public void softDelete() {

		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}

	// =========================================================
	// RESTORE
	// =========================================================

	public void restore() {

		this.deleted = false;
		this.deletedAt = null;
	}
}