package com.tutorial.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Authentication getAuthentication() {

		return SecurityContextHolder.getContext().getAuthentication();
	}

	public static String getCurrentUsername() {

		Authentication authentication = getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {

			return null;
		}

		return authentication.getName();
	}

	public static boolean isAuthenticated() {

		Authentication authentication = getAuthentication();

		return authentication != null && authentication.isAuthenticated();
	}
}
