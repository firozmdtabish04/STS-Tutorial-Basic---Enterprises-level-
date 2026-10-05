package com.tutorial.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private final CustomUserDetailsService userDetailsService;

	private final PasswordEncoder passwordEncoder;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		// Get email/username from login request
		String email = authentication.getName();

		// Get raw password from login request
		String password = authentication.getCredentials().toString();

		// Load user from database
		UserDetails userDetails = userDetailsService.loadUserByUsername(email);

		// Check password
		if (!passwordEncoder.matches(password, userDetails.getPassword())) {

			throw new BadCredentialsException("Invalid email or password");
		}

		// Check account enabled
		if (!userDetails.isEnabled()) {

			throw new DisabledException("Account is disabled");
		}

		// Check account locked
		if (!userDetails.isAccountNonLocked()) {

			throw new LockedException("Account is locked");
		}

		// Authentication successful
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}

	@Override
	public boolean supports(Class<?> authentication) {

		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}